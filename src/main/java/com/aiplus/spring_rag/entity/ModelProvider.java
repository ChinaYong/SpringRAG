package com.aiplus.spring_rag.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;

import lombok.Data;

@Data
@TableName(value = "model_provider", autoResultMap = true) // autoResultMap = true 开启映射机制
public class ModelProvider {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private String name;

    @TableField("api_base_url")
    private String apiUrl;

    private String apiKey;

    // mybatis-plus 将 JSON 数组映射为 List<String>
    @TableField(value = "model_list", typeHandler = JacksonTypeHandler.class)
    private List<String> models;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
