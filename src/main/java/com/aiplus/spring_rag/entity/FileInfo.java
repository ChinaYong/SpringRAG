package com.aiplus.spring_rag.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("file_info")
public class FileInfo {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private String name;
    private String path;
    private Long size;
    private FileStatus status;
    private String type;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    enum FileStatus {
        PENDING("pending"),
        PROCESSING("processing"),
        SUCCESS("success"),
        FAILED("failed");

        @EnumValue
        private final String value;

        private FileStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
