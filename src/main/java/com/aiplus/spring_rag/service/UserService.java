package com.aiplus.spring_rag.service;

import com.aiplus.spring_rag.dto.UserLoginDTO;
import com.aiplus.spring_rag.dto.UserRegisterDTO;
import com.aiplus.spring_rag.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

public interface UserService extends IService<User> {
        
    // 用户注册
    public void register(UserRegisterDTO userRegisterDTO);

    // 用户登录
    public String login(UserLoginDTO userLoginDTO);
}
