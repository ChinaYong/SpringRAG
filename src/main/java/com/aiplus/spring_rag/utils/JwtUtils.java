package com.aiplus.spring_rag.utils;

import java.time.Instant;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

public class JwtUtils {
    
    private static final String SECRET_KEY = "AIplus_RAG_App_Secret_Key_2026";
    
    private static final long EXPIRATION_TIME = 7 * 24 * 60 * 60; // JWT 有效期：一周

    // 根据用户ID 生成 JWT
    public static String generateToken(Integer userId) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(EXPIRATION_TIME);

        return JWT.create()
            .withClaim("userId", userId) // 载荷：用户ID
            .withIssuedAt(now) // 签发时间
            .withExpiresAt(expiration) // 过期时间
            .sign(Algorithm.HMAC256(SECRET_KEY)); // 签名
    }

    // 验证 JWT 的有效性并解析
    public static DecodedJWT verifyToken(String token) {
        return JWT.require(Algorithm.HMAC256(SECRET_KEY))
            .build()
            .verify(token);
    }
}
