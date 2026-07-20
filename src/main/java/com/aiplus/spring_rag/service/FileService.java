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

        /**
         * 根据文件 SHA-256 重新存储文件并删除临时文件
         */
        FileStorage fileStorage = new FileStorage();

        // 检查 SHA-256 是否为空
        if (sha256Hash == null || sha256Hash.isBlank()) {
            throw new RuntimeException("SHA-256 计算异常");
        }

        // 分析是否存在相同的文件，如果存在则不再存储，直接引用即可。
        FileStorage existingFileStorage = fileStorageService.getOne(
                new QueryWrapper<FileStorage>().eq("sha256", sha256Hash),
                false);
        if (existingFileStorage != null) {
            log.info("文件已存在，直接引用，无需重复存储");

            // 更新引用计数
            boolean refAdded = fileStorageService
                    .lambdaUpdate()
                    .eq(FileStorage::getId, existingFileStorage.getId())
                    .setSql("ref_count = ref_count + 1")
                    .set(FileStorage::getUpdateTime, LocalDateTime.now())
                    .update();

            fileStorage = refAdded ? existingFileStorage
                    : storeFileAndDeleteTemp(
                            tempFilePath, sha256Hash, fileUploadDTO.getFile());

        } else {
            fileStorage = storeFileAndDeleteTemp(tempFilePath, sha256Hash, fileUploadDTO.getFile());
        }

        fileProgressService.report(
                fileUploadDTO.getTaskId(),
                fileUploadDTO.getUserId(),
                fileStorage.getId(),
                "PROCESSING",
                "STORED",
                15,
                "文件保存成功");

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
        if (fileStorage.getVectorizedStatus() == 2) {
            log.info("文件已向量化，无需重复处理");
            fileUploadResponseDTO.setStatus("success");
            fileProgressService.report(
                    fileUploadDTO.getTaskId(),
                    fileUploadDTO.getUserId(),
                    fileStorage.getId(),
                    "SUCCESS",
                    "COMPLETED",
                    100,
                    "向量化完成");
            return fileUploadResponseDTO;
        }

        // 文件在处理中，等待处理完成
        if (fileStorage.getVectorizedStatus() == 1) {
            log.info("文件正在处理中，等待文件处理完成");
            fileUploadResponseDTO.setStatus("processing");

            // TODO：如果其他用户上传了相同的文件，那么省去重复处理后，就需要将处理进度同步，或者降级成前端轮询监测进度
            fileProgressService.report(
                    fileUploadDTO.getTaskId(),
                    fileUploadDTO.getUserId(),
                    fileStorage.getId(),
                    "PROCESSING",
                    "PARSING",
                    50,
                    "文件处理中");
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

    // 将临时文件迁移到目的地
    private FileStorage storeFileAndDeleteTemp(Path tempFilePath, String sha256Hash, MultipartFile file) {

        FileStorage fileStorage = new FileStorage();

        // 文件不存在，存储文件
        String realPath = getStoragePathBySha256(
                sha256Hash,
                file);

        // 将文件存储到最终路径并删除临时文件
        streamFileTransfer(tempFilePath, Path.of(realPath));

        // 保存文件信息到数据库
        fileStorage.setSha256(sha256Hash);
        fileStorage.setStoragePath(realPath);
        fileStorage.setSize(file.getSize());
        fileStorage.setExtension(FileUtils.getFileExtension(realPath));
        fileStorage.setRefCount(1);
        fileStorage.setVectorizedStatus((short) 0); // 初始状态为未向量化
        fileStorageService.save(fileStorage);

        return fileStorage;
    }
}
