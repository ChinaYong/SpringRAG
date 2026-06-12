package com.aiplus.spring_rag.interceptor;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.aiplus.spring_rag.utils.JwtUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) 
        throws Exception {
            String token = request.getHeader("Authorization");
            if (token == null || token.isBlank()) {
                throw new RuntimeException("请携带有效的 token 登录！");
            }

            JwtUtils.verifyToken(token);
            return true;
        }
}
