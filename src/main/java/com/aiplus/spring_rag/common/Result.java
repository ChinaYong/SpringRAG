package com.aiplus.spring_rag.common;

import lombok.Data;

@Data
public class Result<T> {
    
    // 状态码: 200代表成功
    private Integer code;

    private String message;

    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("sucess!");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
