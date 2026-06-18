package com.aiplus.spring_rag.dto;

import java.util.Map;

import lombok.Data;

@Data
/**
 * 前端对话请求的载体
 */
public class ChatRequestDTO {
    
    private Integer sessionId;  
    private String content;
    private Integer agentId;
    private Integer order;
    private String modelId; // model 的型号
    private Integer[] fileIds;
    private Map<String, Object> modelParameters;
    private Integer maxContext;
    
}
    
