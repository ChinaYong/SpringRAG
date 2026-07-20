package com.aiplus.spring_rag.dto;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class FileUploadDTO {

    private Integer sessionId; // 文件在对话中上传所属的会话 id
    private Integer order; // 通过sessionId 与 order 定位文件上传的对话
    private Integer agentId; // 文件作为智能体的知识库上传需要绑定智能体 id，与 sessionId 和 agentId 互斥
    private String fileRealType; // 认为存放文件的真实扩展名，后续如果校验与该字段不符，则认为是非法文件类型

    private Integer userId; // 文件上传的用户 id
    private MultipartFile file; // 上传的文件

    private String taskId; // 文件处理进度的任务 id
}
