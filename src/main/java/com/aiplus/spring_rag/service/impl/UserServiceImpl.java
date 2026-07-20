package com.aiplus.spring_rag.service.impl;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.aiplus.spring_rag.dto.UserLoginDTO;
import com.aiplus.spring_rag.dto.UserRegisterDTO;
import com.aiplus.spring_rag.entity.User;
import com.aiplus.spring_rag.mapper.UserMapper;
import com.aiplus.spring_rag.service.UserService;
import com.aiplus.spring_rag.utils.JwtUtils;
import com.aiplus.spring_rag.utils.PasswordUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void register(UserRegisterDTO userRegisterDTO) {
        if (this.lambdaQuery().eq(User::getUsername, userRegisterDTO.getUsername()).one() != null) {
            throw new RuntimeException("用户名已存在!");
        }
        // 加密密码
        userRegisterDTO.setPassword(PasswordUtils.hashPassword(userRegisterDTO.getPassword()));
        // 添加用户
        User user = new User(userRegisterDTO);
        this.save(user);
    }

    @Override
    public String login(UserLoginDTO userLoginDTO) {
        User user = this.lambdaQuery().eq(User::getUsername, userLoginDTO.getUsername()).one();
        if (user == null) {
            throw new RuntimeException("用户名或密码错误！");
        }
        if (PasswordUtils.checkPassword(userLoginDTO.getPassword(), user.getPassword())) {
            return JwtUtils.generateToken(user.getId());
        }
        throw new RuntimeException("用户名或密码错误！");
    }

    @Override
    public void logout(String token) {

        if (token != null && !token.isBlank()) {
            stringRedisTemplate.opsForValue().set("blacklist:" + token, "invalid", 7, TimeUnit.DAYS);
        }
    }
}
