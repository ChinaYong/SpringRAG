package com.aiplus.spring_rag.dto;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AgentDTO {

    @NotBlank(message = "模型名称不能为空")
    private String name;

    private String description;

    private String systemPrompt;

    private Integer modelId;

    private Map<String, Object> modelParameters;

    // 知识库文件列表
    private List<Integer> fileIds;
}
