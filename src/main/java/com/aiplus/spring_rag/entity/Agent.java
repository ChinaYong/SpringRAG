package com.aiplus.spring_rag.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.aiplus.spring_rag.dto.AgentDTO;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;

import lombok.Data;

@Data
@TableName(value = "agent", autoResultMap = true)
public class Agent {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private String name;
    private String description;
    private String systemPrompt;
    private Integer modelId;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> modelParameters;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Integer> fileIds;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public Agent() {}

    public Agent(AgentDTO agentDTO) {
        this.name = agentDTO.getName();
        this.description = agentDTO.getDescription();
        this.systemPrompt = agentDTO.getSystemPrompt();
        this.modelId = agentDTO.getModelId();
        this.modelParameters = agentDTO.getModelParameters();
        this.fileIds = agentDTO.getFileIds();
    }
}
