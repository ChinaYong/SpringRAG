package com.aiplus.spring_rag.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("model_provider")
public class ModelProvider {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private String name;

    @TableField("api_base_url")
    private String apiUrl;

    private String apiKey;

    @TableField("model_list")
    private String models;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
