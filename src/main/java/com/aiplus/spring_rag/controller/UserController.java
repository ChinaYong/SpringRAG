package com.aiplus.spring_rag.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aiplus.spring_rag.common.Result;
import com.aiplus.spring_rag.dto.UserLoginDTO;
import com.aiplus.spring_rag.dto.UserRegisterDTO;
import com.aiplus.spring_rag.service.UserService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user")
public class UserController {
    
    private final UserService userService;

    @PostMapping("/register") //主要仅限 POST 请求，GET 请求会将信息暴露在 URL 中。
    public Result<String> register(@RequestBody UserRegisterDTO userRegisterDTO) {
        userService.register(userRegisterDTO);
        return Result.success("注册成功");
    }

    @PostMapping("/login")
    public Result<String> login(@RequestBody UserLoginDTO userLoginDTO) {
        String token = userService.login(userLoginDTO);
        return Result.success(token);
    }
}
