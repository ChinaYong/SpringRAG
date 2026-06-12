package com.aiplus.spring_rag.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.aiplus.spring_rag.entity.User;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    @Select("select * from user where username = #{username}")
    public User selectUserByUsername(@Param("username") String username);
}