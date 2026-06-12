package com.aiplus.spring_rag.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("chat_message")
public class ChatMessage {
    private Integer sessionId;
    private Role role;
    private String content;
    private Integer tokens;
    private Integer order;
    private String fileIds;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public enum Role {
        USER("user"),
        ASSISTANT("assistant");

        @EnumValue
        private final String value;

        private Role(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
