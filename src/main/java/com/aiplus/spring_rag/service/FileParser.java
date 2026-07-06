package com.aiplus.spring_rag.service;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.aiplus.spring_rag.dto.FileHandleDTO;
import com.aiplus.spring_rag.dto.FileUploadDTO;

/**
 * 文件的解析
 */
@Component
public class FileParser {

    // text 文件解析
    public static List<Document> parseTextFile(FileHandleDTO fileHandleDTO) {
        Resource resource = new FileSystemResource(fileHandleDTO.getFilePath());
        TextReader textReader = new TextReader(resource);

        textReader.setCharset(Charset.forName("UTF-8"));

        // TODO：在此处设置每个文件的自定义的公共元数据
        FileUploadDTO fileUploadDTO = fileHandleDTO.getFileUploadDTO();
        Map<String, Object> customMetadata = textReader.getCustomMetadata();
        customMetadata.put("user_id", fileUploadDTO.getUserId().toString());
        customMetadata.put("file_name", fileUploadDTO.getFile().getOriginalFilename());
        customMetadata.put("file_id", fileHandleDTO.getFileId());

        return textReader.read();

    }

    // pdf 文件解析
    

}
