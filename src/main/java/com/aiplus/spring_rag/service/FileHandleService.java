package com.aiplus.spring_rag.service;

import com.aiplus.spring_rag.dto.FileHandleDTO;
import com.aiplus.spring_rag.entity.FileStorage;
import com.aiplus.spring_rag.utils.FileUtils;

import jakarta.annotation.PostConstruct;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileHandleService {

    private final FileStorageService fileStorageService;

    private final VectorStore vectorStore;

    private final FileParser fileParser;

    private final FileSplitter fileSplitter;

    private final FileProgressService fileProgressService;

    // 解析策略
    private Map<String, Function<FileHandleDTO, List<Document>>> fileHandleMap;

    // 分块策略
    private Map<String, Function<List<Document>, List<Document>>> fileSplitterMap;

    @PostConstruct
    private void init() {
        fileHandleMap = Map.ofEntries(
                Map.entry(".txt", fileParser::parseTextFile),
                Map.entry(".md", fileParser::parseMarkdownFile));

        fileSplitterMap = Map.ofEntries(
                Map.entry(".txt", fileSplitter::splitText));
    }

    /**
     * 文件处理入口
     */
    @Async("fileHandleExecutor")
    public void handle(FileHandleDTO fileHandleDTO) {
        // 设置文件向量化状态为解析中
        fileStorageService
                .lambdaUpdate()
                .eq(FileStorage::getId, fileHandleDTO.getFileId())
                .set(FileStorage::getVectorizedStatus, (short) 1)
                .update();

        try {
            String extensionName = FileUtils.getFileExtension(
                    fileHandleDTO.getFilePath());
            if (extensionName == null || extensionName.isBlank()) {
                throw new RuntimeException("文件扩展名为空");
            }

            if (!fileHandleMap.containsKey(extensionName)) {
                throw new RuntimeException("不支持文件解析的类型");
            }

            // 更新文件处理进度
            fileProgressService.report(
                    fileHandleDTO.getFileUploadDTO().getTaskId(),
                    fileHandleDTO.getFileUploadDTO().getUserId(),
                    fileHandleDTO.getFileId(),
                    "PROCESSING",
                    "PARSING",
                    40,
                    "文件解析中");
            // 文件解析
            List<Document> documents = fileHandleMap
                    .get(extensionName)
                    .apply(fileHandleDTO);

            // 更新文件处理进度
            fileProgressService.report(
                    fileHandleDTO.getFileUploadDTO().getTaskId(),
                    fileHandleDTO.getFileUploadDTO().getUserId(),
                    fileHandleDTO.getFileId(),
                    "PROCESSING",
                    "SPLITTING",
                    50,
                    "文件分块中");

            if (!fileSplitterMap.containsKey(extensionName)) {
                throw new RuntimeException("不支持文件分块的类型");
            }
            // 文本分块
            List<Document> chunks = fileSplitterMap
                    .get(extensionName)
                    .apply(documents);

            // 更新文件处理进度
            fileProgressService.report(
                    fileHandleDTO.getFileUploadDTO().getTaskId(),
                    fileHandleDTO.getFileUploadDTO().getUserId(),
                    fileHandleDTO.getFileId(),
                    "PROCESSING",
                    "EMBEDDING",
                    60,
                    "文件向量化中");
            // 写入到向量数据库
            vectorStore.add(chunks);

            // 更新文件向量化状态为已完成
            fileStorageService
                    .lambdaUpdate()
                    .eq(FileStorage::getId, fileHandleDTO.getFileId())
                    .set(FileStorage::getVectorizedStatus, (short) 2)
                    .update();

            // 更新文件处理进度
            fileProgressService.report(
                    fileHandleDTO.getFileUploadDTO().getTaskId(),
                    fileHandleDTO.getFileUploadDTO().getUserId(),
                    fileHandleDTO.getFileId(),
                    "SUCCESS",
                    "COMPLETED",
                    100,
                    "文件处理完成");
        } catch (Exception e) {
            log.error("文件处理失败 fileId={}", fileHandleDTO.getFileId(), e);
            // 更新文件向量化状态为失败
            fileStorageService
                    .lambdaUpdate()
                    .eq(FileStorage::getId, fileHandleDTO.getFileId())
                    .set(FileStorage::getVectorizedStatus, (short) 3)
                    .update();

            fileProgressService.report(
                    fileHandleDTO.getFileUploadDTO().getTaskId(),
                    fileHandleDTO.getFileUploadDTO().getUserId(),
                    fileHandleDTO.getFileId(),
                    "FAILED",
                    "COMPLETED",
                    100,
                    "文件处理失败");

            // TODO：后续可以将这串代码用在删除文件的逻辑中
            // 删除引用计数为 0 的文件
            // FileStorage fileStorage = fileStorageService.getById(
            // fileHandleDTO.getFileId()
            // );
            // if (fileStorage != null && fileStorage.getRefCount() <= 0) {
            // // 删除数据库中的相关记录
            // fileStorageService.removeById(fileHandleDTO.getFileId());
            // userFileInfoService
            // .lambdaUpdate()
            // .eq(
            // UserFileInfo::getUserId,
            // fileHandleDTO.getFileUploadDTO().getUserId()
            // )
            // .eq(UserFileInfo::getFileId, fileHandleDTO.getFileId())
            // .remove();

            // try {
            // Files.deleteIfExists(Path.of(fileStorage.getStoragePath()));
            // } catch (IOException e1) {
            // log.error(
            // "文件删除失败 fileId={}",
            // fileHandleDTO.getFileId(),
            // e1
            // );
            // }
            // }
        }
    }
}
