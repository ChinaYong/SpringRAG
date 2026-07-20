package com.aiplus.spring_rag.service;

import com.aiplus.spring_rag.dto.FileHandleDTO;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * 文件的解析、分块
 */
@Component
@RequiredArgsConstructor
public class FileParser {

    private final MarkdownDocumentReaderConfig markdownDocumentReaderConfig;

    // text 文件解析
    public List<Document> parseTextFile(FileHandleDTO fileHandleDTO) {
        // 读取文件
        Resource resource = new FileSystemResource(fileHandleDTO.getFilePath());
        TextReader textReader = new TextReader(resource);

        // 注入分块公共的元数据
        Map<String, Object> customMetadata = textReader.getCustomMetadata();
        customMetadata.put("file_id", fileHandleDTO.getFileId());
        customMetadata.put("file_path", fileHandleDTO.getFilePath()); // Spring AI 在填充 source 元数据，存放文件路径，但是是以 file 开头的
        customMetadata.put("user_id", fileHandleDTO.getFileUploadDTO().getUserId());
        customMetadata.put("file_name", fileHandleDTO.getFileUploadDTO().getFile().getOriginalFilename());
        return textReader.read();
    }

    // md 文件解析
    public List<Document> parseMarkdownFile(FileHandleDTO fileHandleDTO) {
        Resource resource = new FileSystemResource(fileHandleDTO.getFilePath());
        MarkdownDocumentReader markdownDocumentReader = new MarkdownDocumentReader(
                resource,
                markdownDocumentReaderConfig);
        return markdownDocumentReader.read();
    }
    // pdf 文件解析
}
