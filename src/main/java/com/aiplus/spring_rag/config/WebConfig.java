package com.aiplus.spring_rag.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.aiplus.spring_rag.interceptor.JwtInterceptor;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor) // 添加拦截器
            .addPathPatterns("/api/**") // 添加连接路径
            .excludePathPatterns(
                "/api/user/register", // 注册
                "/api/user/login" // 登录
            ); // 排除路径
    }
}
