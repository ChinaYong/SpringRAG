package com.aiplus.spring_rag.dto;

import lombok.Data;

/**
 * 文件处理时的 DTO
 */
@Data
public class FileHandleDTO {
    
    private FileUploadDTO fileUploadDTO; // 文件上传的 DTO

    private Integer fileId; // 文件在数据库中的唯一标识

    private String filePath; // 文件在服务器上的存储路径
    
    public FileHandleDTO(FileUploadDTO fileUploadDTO) {
        this.fileUploadDTO = fileUploadDTO;
    }
}
