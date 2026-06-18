package com.aiplus.spring_rag.dto;

import lombok.Data;

@Data
public class FileUploadResponseDTO {
    
    private Integer fileId;     // 数据库存储的文件 id
    private String fileName;    // 文件名
    private Long size;      // 文件大小 KB
    private String type;    // 文件类型
    private String status;  // 处理状态：pending / processing / success / failed
    private String filePath;    // 文件路径，方便在历史记录中查看，如果引入需要对访问者鉴权

}
