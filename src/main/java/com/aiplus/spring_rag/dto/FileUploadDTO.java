package com.aiplus.spring_rag.dto;

import lombok.Data;

@Data
public class FileUploadDTO {
    
    private Integer sessionId; // 文件在对话中上传所属的会话 id
    private Integer order;  // 通过sessionId 与 order 定位文件上传的对话
    private Integer agentId; // 文件作为智能体的知识库上传需要绑定智能体 id，与 sessionId 和 agentId 互斥
    private String fileRealType;    // 存放文件的真实扩展名
}
