package com.aiplus.spring_rag.interceptor;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.aiplus.spring_rag.utils.JwtUtils;
import com.auth0.jwt.interfaces.DecodedJWT;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {
    
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) 
        throws Exception {
            String token = request.getHeader("Authorization");
            if (token == null || token.isBlank()) {
                throw new RuntimeException("请携带有效的 token 登录！");
            }

            String isBlackList = stringRedisTemplate.opsForValue().get("blacklist:" + token);
            if (isBlackList != null) {
                throw new RuntimeException("用户登录凭证已过期，请重新登录！");
            }
            DecodedJWT decodedJWT = JwtUtils.verifyToken(token);
            Integer userId = decodedJWT.getClaim("userId").asInt();
            request.setAttribute("userId", userId);
            return true;
        }
}
