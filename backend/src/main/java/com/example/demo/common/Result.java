package com.example.demo.common;

import lombok.Data;

import java.util.Map;

/**
 * 统一响应体
 */
@Data
public class Result<T> {

    public static final int CODE_SUCCESS = 200;
    public static final int CODE_BAD_REQUEST = 400;
    public static final int CODE_UNAUTHORIZED = 401;
    public static final int CODE_FORBIDDEN = 403;
    public static final int CODE_NOT_FOUND = 404;
    public static final int CODE_SERVER_ERROR = 500;

    public static final String MSG_SUCCESS = "success";

    private int code;
    private String message;
    private T data;
    private Map<String, String> errors;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = CODE_SUCCESS;
        r.message = MSG_SUCCESS;
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
        return fail(CODE_SERVER_ERROR, message);
    }

    public static <T> Result<T> badRequest(String message) {
        return fail(CODE_BAD_REQUEST, message);
    }

    public static <T> Result<T> forbidden(String message) {
        return fail(CODE_FORBIDDEN, message);
    }

    public static <T> Result<T> notFound(String message) {
        return fail(CODE_NOT_FOUND, message);
    }

    public boolean isSuccess() {
        return code == CODE_SUCCESS;
    }
}
