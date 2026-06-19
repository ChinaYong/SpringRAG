package com.aiplus.spring_rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

public class FileStorage {
    
    @TableId(type = IdType.AUTO)
    private Integer id;     // 主键
    private String sha256;  // 文件 SHA-256
    private String storage_path;    // 基于 SHA-256 的存储路径
    private Long size;       // 文件大小
    private String extension;   // 文件真实扩展名
    private int refCount;    // 文件引用计数
    private short vectorizedStatus;   // 文件向量化的状态
    
}
