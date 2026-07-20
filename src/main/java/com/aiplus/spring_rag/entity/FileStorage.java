package com.aiplus.spring_rag.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("file_storage")
public class FileStorage {
    
    @TableId(type = IdType.AUTO)
    private Integer id;     // 主键
    private String sha256;  // 文件 SHA-256
    private String storagePath;    // 基于 SHA-256 的存储路径
    private Long size;       // 文件大小
    private String extension;   // 文件真实扩展名
    private int refCount;    // 文件引用计数
    private short vectorizedStatus;   // 文件向量化的状态，0:待解析 1:解析中 2:已完成 3:失败
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;   // 创建时间
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;   // 更新时间


}
