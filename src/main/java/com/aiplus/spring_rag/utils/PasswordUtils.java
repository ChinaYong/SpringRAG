package com.aiplus.spring_rag.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordUtils {
     
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private static String sha256Hex(String plainPassword) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            // 获取长度为 256/8 = 32 B的字节数组
            byte[] hash = md.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                // 获取一个 byte 的十六进制表示，考虑到高位可能为0，所以需要补0
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256算法不可用", e);
        }
    }

    // 生成密码哈希
    public static String hashPassword(String plainPassword) {
        return ENCODER.encode(sha256Hex(plainPassword));
    }

    // 检查密码是否正确
    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        return ENCODER.matches(sha256Hex(plainPassword), hashedPassword);
    }
}
