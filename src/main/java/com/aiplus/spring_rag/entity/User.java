package com.aiplus.spring_rag.entity;

import java.time.LocalDateTime;

import com.aiplus.spring_rag.dto.UserRegisterDTO;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data // Lombok 注解，自动生成 setter、getter、toString
@TableName("user") // MyBatis-Plus 注解，指定表名
public class User {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String username;
    private String password;

    @TableField(fill = FieldFill.INSERT) // 插入时自动填充
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE) // 插入和更新时自动填充
    private LocalDateTime updateTime;

    public User() {
    }

    public User(UserRegisterDTO userRegisterDTO) {
        this.username = userRegisterDTO.getUsername();
        this.password = userRegisterDTO.getPassword();
    }
    
}
