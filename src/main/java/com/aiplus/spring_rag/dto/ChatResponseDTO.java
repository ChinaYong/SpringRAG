package com.aiplus.spring_rag.dto;

import lombok.Data;

@Data
public class ChatResponseDTO {
    
    private String content;
    private boolean isFinished; // 流式响应的结束标志
    private Usage usage;    // 记录 token 的消耗

    public static class Usage {
        private Integer promptTokens;   // 提示词消耗 token
        private Integer completionTokens;   // 生成回复消耗 token
        private Integer totalTokens;    // 此次消耗的总 token
    }
}
