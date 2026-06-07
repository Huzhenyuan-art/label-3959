package com.example.demo.common;

import lombok.Data;

import java.util.Map;

/**
 * 统一响应体
 */
@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;
    private Map<String, String> errors;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        return r;
    }

    public static <T> Result<T> fail(int code, String message, Map<String, String> errors) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        r.errors = errors;
        return r;
    }

    public static <T> Result<T> fail(String message) {
        return fail(500, message);
    }
}
