package com.aiplus.spring_rag.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.aiplus.spring_rag.dto.FileHandleDTO;
import com.aiplus.spring_rag.dto.FileUploadDTO;
import com.aiplus.spring_rag.entity.FileStorage;
import com.aiplus.spring_rag.utils.FileUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    @Value("${file.storage.base-path}")
    private Path basePath;

    @Value("${file.storage.temp-path}")
    private Path tempPath;

    private final FileStorageService fileStorageService;

    private final VectorStore vectorStore;

    /**
     * 上传文件通用处理
     * ！TODO: 将 MurtipartFile 放到 DTO 中的重构
     */
    public void uploadFileHandle(FileUploadDTO fileUploadDTO, Integer userId) {
        // 文件基础检查
        // /TODO：考虑存在相同文件的情形，即可省略大部分存储过程，但是相同文件的判断需要计算SHA256，前端计算不可信，放弃此优化
        if (fileUploadDTO.getFile().isEmpty()) {
            throw new RuntimeException("文件为空！");
        }
        if (!FileUtils.isAllowedFileType(fileUploadDTO.getFile().getOriginalFilename())) {
            throw new RuntimeException("非法文件类型！");
        }

        // 创建唯一临时文件
        Path tempFilePath = null;
        try {
            // 方法会尝试一定次数自动生成唯一文件名
            tempFilePath = Files.createTempFile(tempPath, userId + "_", ".tmp");
        } catch (IOException e) {
            log.error("临时文件创建失败，重点检查重复文件名！");
            e.printStackTrace();
            throw new RuntimeException("临时文件创建失败");
        }

        // 文件存到磁盘同时求 SHA-256
        String sha256Hash = computeSha256AndStoreFile(fileUploadDTO.getFile(), tempFilePath);

        // 检查文件的合法性
        FileUtils.isExtensionMatchMagicType(tempFilePath.toString());

        // 根据文件 SHA-256 重新存储文件并删除临时文件
        String realPath = null;
        FileStorage fileStorage = new FileStorage();

        // 检查 SHA-256 是否为空
        if (sha256Hash == null || sha256Hash.isBlank()) {
            throw new RuntimeException("SHA-256 计算异常");
        }

        // 分析是否存在相同的文件，如果存在则不再存储，直接引用即可。
        FileStorage existingFileStorage = fileStorageService.getOne(new QueryWrapper<FileStorage>().eq("sha256", sha256Hash), false);
        if (existingFileStorage != null) {
            log.info("文件已存在，直接引用，无需重复存储");
            // 更新引用计数
            existingFileStorage.setRefCount(existingFileStorage.getRefCount() + 1);
            fileStorageService.updateById(existingFileStorage);
            fileStorage = existingFileStorage;

            if (fileStorage.getVectorizedStatus() == 2) {
                // 文件已向量化，无需重复处理
                log.info("文件已向量化，无需重复处理");
            }
        } else {
            // 文件不存在，存储文件
            realPath = getStoragePathBySha256(sha256Hash, fileUploadDTO.getFile());

            // 将文件存储到最终路径并删除临时文件
            streamFileTransfer(tempFilePath, Path.of(realPath));

            // 保存文件信息到数据库
            fileStorage.setSha256(sha256Hash);
            fileStorage.setStoragePath(realPath);
            fileStorage.setSize(fileUploadDTO.getFile().getSize());
            fileStorage.setExtension(FileUtils.getFileExtension(realPath));
            fileStorage.setRefCount(1);
            fileStorage.setVectorizedStatus((short) 0); // 初始状态为未向量化
            fileStorageService.save(fileStorage);
        }

        // 构建文件处理 DTO
        FileHandleDTO fileHandleDTO = new FileHandleDTO(fileUploadDTO);
        fileHandleDTO.setFileId(fileStorage.getId());
        fileHandleDTO.setFilePath(fileStorage.getStoragePath());

        // 文件处理
        fileHandle(fileHandleDTO);

        // 对于文本文件，将文件分块并向量化存储到 Qdrant。

        // 对于 PDF 文件，将文件文本提取出来后分块并向量化存储到 Qdrant

        // 对于图片（jpg/png），暂时存储到本地，后续判断 图片要发送给的模型，是否支持图片？如果支持就将图片和对话一起发送给多模态模型。

        // 如果是智能体知识库上传，需要更新智能体 fileIds


    }


    /**
     * 文件处理入口
     * ！TODO: 处理内容大同小异，考虑用一个模板来实现此类逻辑
     */
    public void fileHandle(FileHandleDTO fileHandleDTO) {

        // 设置文件向量化状态为解析中
        FileStorage fileStorage = fileStorageService.getById(fileHandleDTO.getFileId());
        fileStorage.setVectorizedStatus((short) 1);
        fileStorageService.updateById(fileStorage);

        List<Document> documents = null;
        String extensionName = FileUtils.getFileExtension(fileHandleDTO.getFilePath());

        if (extensionName == null || extensionName.isBlank()) {
            throw new RuntimeException("文件扩展名为空");
        }

        // 根据文件类型进行不同的处理
        // 解析
        Map<String, Function<FileHandleDTO, List<Document>>> fileHandleMap = Map.of(
            ".txt", FileParser::parseTextFile
        );
        // 分块
        Map<String, Function<List<Document>, List<Document>>> fileSplitterMap = Map.of(
            ".txt", FileSplitter::splitDocuments
        );
        // 文档解析
        documents = fileHandleMap.get(extensionName).apply(fileHandleDTO);

        // 文件分块
        List<Document> chunks = fileSplitterMap.get(extensionName).apply(documents);

        // 写入到向量数据库
        vectorStore.add(chunks);

        // 更新文件向量化状态为已完成
        fileStorage.setVectorizedStatus((short) 2);
        fileStorageService.updateById(fileStorage);
    }


    /**
     * 根据 SHA-256 创建好的存储路径，返回存储路径
     */
    private String getStoragePathBySha256(String sha256Hash, MultipartFile file) {
        // 获取一级、二级目录
        String firstDirectory = sha256Hash.substring(0, 2);
        String secondDirectory = sha256Hash.substring(2, 4);

        // 拼接完整路径
        String fileExtensionName;
        int extensionIdx = file.getOriginalFilename().lastIndexOf(".");
        if (extensionIdx != -1) {
            fileExtensionName = file.getOriginalFilename().substring(extensionIdx);
        } else {
            fileExtensionName = "";
            throw new RuntimeException("非法文件扩展名");
        }

        // 如果不存在，就提前创建好目录
        Path targetPath = Path.of(basePath.toString() + "//" + firstDirectory + "//" + secondDirectory);
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
        return targetPath.toString()
            + "//" + sha256Hash + fileExtensionName;
    }

    /**
     * 流式文件迁移
     */
    private void streamFileTransfer(Path sourcePath, Path targetPath) {
        // 存储文件
        try (InputStream inputStream = Files.newInputStream(sourcePath);
                OutputStream outputStream = Files.newOutputStream(targetPath)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while((bytesRead = inputStream.read(buffer)) != -1) {
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
    private String computeSha256AndStoreFile(MultipartFile file, Path tempFilePath) {

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("文件为空！");
        }
        if (tempFilePath == null) {
            throw new RuntimeException("临时文件路径为空！");
        }

        String sha256Hash = null;
        try (InputStream inputStream = file.getInputStream();
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
        } catch (IOException e){
            log.error(e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("上传文件读取异常");
        } catch (NoSuchAlgorithmException e) {
            log.error(e.getMessage());
            throw new RuntimeException("未找到 SHA-256 算法，JDK 异常");
        }
        return sha256Hash;
    }
}
