package com.aiplus.spring_rag.service;

import com.aiplus.spring_rag.dto.FileHandleDTO;
import com.aiplus.spring_rag.dto.FileUploadDTO;
import com.aiplus.spring_rag.dto.FileUploadResponseDTO;
import com.aiplus.spring_rag.entity.FileStorage;
import com.aiplus.spring_rag.utils.FileUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    @Value("${file.storage.base-path}")
    private Path basePath;

    @Value("${file.storage.temp-path}")
    private Path tempPath;

    private final FileStorageService fileStorageService;

    private final FileHandleService fileHandleService;

    private final UserFileInfoService userFileInfoService;

    private final AgentService agentService;

    private final FileProgressService fileProgressService;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 上传文件通用处理
     */
    public FileUploadResponseDTO uploadFileHandle(FileUploadDTO fileUploadDTO) {
        // 文件基础检查
        if (fileUploadDTO.getFile().isEmpty()) {
            throw new RuntimeException("文件为空！");
        }
        if (!FileUtils.isAllowedFileType(
                fileUploadDTO.getFile().getOriginalFilename())) {
            throw new RuntimeException("非法文件类型！");
        }

        // 创建唯一临时文件
        Path tempFilePath = null;
        try {
            // 方法会尝试一定次数自动生成唯一文件名
            tempFilePath = Files.createTempFile(
                    tempPath,
                    fileUploadDTO.getUserId() + "_",
                    "." + FileUtils.getFileExtension(fileUploadDTO.getFile().getOriginalFilename().toString()));
        } catch (IOException e) {
            log.error("临时文件创建失败，重点检查重复文件名！");
            e.printStackTrace();
            throw new RuntimeException("临时文件创建失败");
        }

        // 文件存到磁盘同时求 SHA-256
        String sha256Hash = computeSha256AndStoreFile(
                fileUploadDTO.getFile(),
                tempFilePath);

        // 检查文件的合法性
        if (!FileUtils.isExtensionMatchMagicType(tempFilePath.toString())) {
            throw new RuntimeException("不支持的文件类型！");
        }

        // 检查 SHA-256 是否为空
        if (sha256Hash == null || sha256Hash.isBlank()) {
            throw new RuntimeException("SHA-256 计算异常");
        }

        /**
         * 先插入一条只包含 SHA-256 的占位记录。
         * 数据库中的 SHA-256 唯一索引负责决定哪个并发请求是新文件的存储者。
         */
        FileStorage reserved = new FileStorage();
        reserved.setSha256(sha256Hash);
        reserved.setRefCount(1);
        reserved.setVectorizedStatus((short) 0);

        boolean newFile;
        try {
            newFile = fileStorageService.save(reserved);
            if (!newFile) {
                throw new RuntimeException("文件存储占位记录创建失败");
            }
        } catch (DuplicateKeyException e) {
            // 唯一键冲突说明其他请求已经为相同 SHA-256 创建了记录。
            newFile = false;
            reserved = fileStorageService.getOne(
                    new QueryWrapper<FileStorage>().eq("sha256", sha256Hash),
                    false);

            if (reserved == null) {
                throw new RuntimeException("重复文件记录查询失败", e);
            }
        }

        if (!Boolean.TRUE.equals(newFile)) {
            log.info("文件已存在，直接引用，无需重复存储");

            // 更新引用计数
            boolean refAdded = fileStorageService
                    .lambdaUpdate()
                    .eq(FileStorage::getId, reserved.getId())
                    .setSql("ref_count = ref_count + 1")
                    .set(FileStorage::getUpdateTime, LocalDateTime.now())
                    .update();

            if (!refAdded) {
                throw new RuntimeException("文件引用计数更新失败");
            }

            // 最多等文件存储 10s
            int i = 0;
            for (; i < 10; i++) {
                reserved = fileStorageService
                        .getOne(
                                new QueryWrapper<FileStorage>().eq("sha256", sha256Hash),
                                false);

                if (reserved.getStoragePath() != null
                        && !reserved.getStoragePath().isBlank()) {
                    break;
                } else {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("线程等待中断", e);
                        throw new RuntimeException("线程等待中断");
                    }
                }
            }

            if (i == 10) {
                throw new RuntimeException("文件存储超时，请重试");
            }

            // 重复文件直接复用正式文件，本次上传产生的临时文件不再需要。
            deleteTempFile(tempFilePath);
        } else {
            try {
                reserved = storeFileAndDeleteTemp(
                        reserved,
                        tempFilePath,
                        sha256Hash,
                        fileUploadDTO.getFile());
            } catch (RuntimeException e) {
                /*
                 * 新文件存储失败时删除尚未完成的占位记录，
                 * 否则后续相同 SHA-256 的上传会一直等待 storage_path。
                 */
                fileStorageService.remove(
                        new QueryWrapper<FileStorage>()
                                .eq("id", reserved.getId())
                                .isNull("storage_path"));
                throw e;
            }
            // reportWithFile 会先保存文件级最新快照，再向该文件的所有 task 广播。
            fileProgressService.reportWithFile(
                    reserved.getId(),
                    "PROCESSING",
                    "STORED",
                    15,
                    "文件保存成功");
        }

        // 从这里开始，文件记录已经包含完整的正式存储信息。
        FileStorage fileStorage = reserved;

        // Redis: 存储 fileId 与 taskId 的关系
        String redisTasksKey = fileProgressService.buildTasksKey(fileStorage.getId());
        stringRedisTemplate.opsForSet().add(redisTasksKey, fileUploadDTO.getTaskId());

        // 保存 user_file_info 表的记录
        userFileInfoService.saveUserFileRef(fileUploadDTO.getUserId(), fileStorage.getId());

        // 保存 agent 表的记录：向 agent.file_ids(JSON 数组) 追加本次上传的文件ID
        // userId 来自 JWT（服务端写入），可信任；同时用于校验 agent 归属，防止越权
        if (fileUploadDTO.getAgentId() != null) {
            agentService.appendFileId(fileUploadDTO.getUserId(), fileUploadDTO.getAgentId(), fileStorage.getId());
        }

        FileUploadResponseDTO fileUploadResponseDTO = new FileUploadResponseDTO();
        fileUploadResponseDTO.setFileId(fileStorage.getId());
        fileUploadResponseDTO.setFileName(fileUploadDTO.getFile().getOriginalFilename());
        fileUploadResponseDTO.setSize(fileUploadDTO.getFile().getSize());
        fileUploadResponseDTO.setType(fileUploadDTO.getFile().getContentType());
        fileUploadResponseDTO.setStatus("pending");
        fileUploadResponseDTO.setFilePath(fileStorage.getStoragePath());

        // 文件已向量化，无需重复处理
        if (!newFile && fileStorage.getVectorizedStatus() == 2) {
            log.info("文件已向量化，无需重复处理");
            fileUploadResponseDTO.setStatus("success");
            fileProgressService.reportWithFile(
                    fileStorage.getId(),
                    "SUCCESS",
                    "COMPLETED",
                    100,
                    "向量化完成");

            stringRedisTemplate.opsForSet().remove(redisTasksKey, fileUploadDTO.getTaskId());
            return fileUploadResponseDTO;
        }

        // 文件在处理中，等待处理完成
        if (!newFile || fileStorage.getVectorizedStatus() == 1) {
            log.info("文件正在处理中，等待文件处理完成");
            fileUploadResponseDTO.setStatus("processing");

            return fileUploadResponseDTO;
        }

        // 构建文件处理 DTO
        FileHandleDTO fileHandleDTO = new FileHandleDTO(fileUploadDTO);
        fileHandleDTO.setFileId(fileStorage.getId());
        fileHandleDTO.setFilePath(fileStorage.getStoragePath());

        // 文件处理
        fileHandleService.handle(fileHandleDTO);

        fileUploadResponseDTO.setStatus("processing");

        return fileUploadResponseDTO;
    }

    /**
     * 根据 SHA-256 创建好的存储路径，返回存储路径
     */
    private String getStoragePathBySha256(
            String sha256Hash,
            MultipartFile file) {
        // 获取一级、二级目录
        String firstDirectory = sha256Hash.substring(0, 2);
        String secondDirectory = sha256Hash.substring(2, 4);

        // 拼接完整路径
        String fileExtensionName;
        int extensionIdx = file.getOriginalFilename().lastIndexOf(".");
        if (extensionIdx != -1) {
            fileExtensionName = file
                    .getOriginalFilename()
                    .substring(extensionIdx);
        } else {
            fileExtensionName = "";
            throw new RuntimeException("非法文件扩展名");
        }

        // 如果不存在，就提前创建好目录
        Path targetPath = Path.of(
                basePath.toString() + "//" + firstDirectory + "//" + secondDirectory);
        try {
            Files.createDirectories(targetPath);
        } catch (FileAlreadyExistsException e) {
            log.error(e.getMessage());
            throw new RuntimeException("存在非法文件名导致目录创建失败");
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException("创建目录时 IO 异常");
        }

        // 拼接最终的存储路径
        return targetPath.toString() + "//" + sha256Hash + fileExtensionName;
    }

    /**
     * 流式文件迁移
     */
    private void streamFileTransfer(Path sourcePath, Path targetPath) {
        // 存储文件
        try (
                InputStream inputStream = Files.newInputStream(sourcePath);
                OutputStream outputStream = Files.newOutputStream(targetPath)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException("文件存储到本地异常");
        }

        // 删除临时文件
        try {
            Files.deleteIfExists(sourcePath);
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException("临时文件删除异常");
        }
    }

    /**
     * 文件存到磁盘同时求 SHA-256
     */
    private String computeSha256AndStoreFile(
            MultipartFile file,
            Path tempFilePath) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("文件为空！");
        }
        if (tempFilePath == null) {
            throw new RuntimeException("临时文件路径为空！");
        }

        String sha256Hash = null;
        try (
                InputStream inputStream = file.getInputStream();
                OutputStream outputStream = Files.newOutputStream(tempFilePath)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                digest.update(buffer, 0, bytesRead);
            }

            byte[] hashBytes = digest.digest();
            sha256Hash = FileUtils.getHexString(hashBytes);
        } catch (IOException e) {
            log.error(e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("上传文件读取异常");
        } catch (NoSuchAlgorithmException e) {
            log.error(e.getMessage());
            throw new RuntimeException("未找到 SHA-256 算法，JDK 异常");
        }
        return sha256Hash;
    }

    // 将临时文件迁移到目的地，并补全之前创建的数据库占位记录
    private FileStorage storeFileAndDeleteTemp(
            FileStorage reserved,
            Path tempFilePath,
            String sha256Hash,
            MultipartFile file) {
        // 文件不存在，存储文件
        String realPath = getStoragePathBySha256(
                sha256Hash,
                file);

        // 将文件存储到最终路径并删除临时文件
        streamFileTransfer(tempFilePath, Path.of(realPath));

        /*
         * 只更新文件存储字段，不整体 updateById(reserved)。
         * 在正式文件写入期间，其他重复上传可能已经增加了 ref_count；
         * 局部更新可以避免用 reserved 中的旧值覆盖最新引用计数。
         */
        boolean updated = fileStorageService
                .lambdaUpdate()
                .eq(FileStorage::getId, reserved.getId())
                .isNull(FileStorage::getStoragePath)
                .set(FileStorage::getStoragePath, realPath)
                .set(FileStorage::getSize, file.getSize())
                .set(
                        FileStorage::getExtension,
                        FileUtils.getFileExtension(realPath))
                .set(FileStorage::getUpdateTime, LocalDateTime.now())
                .update();

        if (!updated) {
            throw new RuntimeException("文件存储记录更新失败");
        }

        FileStorage stored = fileStorageService.getById(reserved.getId());
        if (stored == null) {
            throw new RuntimeException("文件存储记录不存在");
        }

        return stored;
    }

    /**
     * 删除当前上传产生的临时文件。
     * 这里只处理临时路径，不会删除 SHA-256 对应的共享正式文件。
     */
    private void deleteTempFile(Path tempFilePath) {
        try {
            Files.deleteIfExists(tempFilePath);
        } catch (IOException e) {
            log.warn("重复文件的临时文件清理失败 path={}", tempFilePath, e);
        }
    }
}
