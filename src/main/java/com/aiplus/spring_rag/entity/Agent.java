package com.aiplus.spring_rag.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("agent")
public class Agent {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private String name;
    private String description;
    private String systemPrompt;
    private Integer modelId;
    private String modelParameters;
    private String fileIds;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
