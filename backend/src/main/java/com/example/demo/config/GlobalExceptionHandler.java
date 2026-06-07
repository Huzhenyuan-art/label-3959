package com.example.demo.config;

import com.example.demo.common.Result;
import com.example.demo.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        return Result.fail(Result.CODE_SERVER_ERROR, "系统内部错误，请稍后重试");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数异常: {}", e.getMessage());
        return Result.badRequest(e.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    public Result<Void> handleValidationException(ValidationException e) {
        log.warn("校验异常: {}, errors: {}", e.getMessage(), e.getErrors());
        return Result.fail(Result.CODE_BAD_REQUEST, e.getMessage(), e.getErrors());
    }

    @ExceptionHandler(SecurityException.class)
    public Result<Void> handleSecurityException(SecurityException e) {
        log.warn("安全异常: {}", e.getMessage());
        return Result.forbidden(e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Result<Void> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("访问拒绝: {}", e.getMessage());
        return Result.forbidden("权限不足，无法访问");
    }

    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public Result<Void> handleAuthenticationException(Exception e) {
        log.warn("认证异常: {}", e.getMessage());
        return Result.fail(Result.CODE_UNAUTHORIZED, "认证失败，请重新登录");
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValidationException(Exception e) {
        Map<String, String> errors = new HashMap<>();
        if (e instanceof MethodArgumentNotValidException ex) {
            ex.getBindingResult().getFieldErrors().forEach(error ->
                    errors.put(error.getField(), error.getDefaultMessage()));
        } else if (e instanceof BindException ex) {
            ex.getBindingResult().getFieldErrors().forEach(error ->
                    errors.put(error.getField(), error.getDefaultMessage()));
        }
        log.warn("参数校验失败: {}", errors);
        return Result.fail(Result.CODE_BAD_REQUEST, "参数校验失败", errors);
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public Result<Void> handleRequestParamException(Exception e) {
        log.warn("请求参数异常: {}", e.getMessage());
        return Result.badRequest("请求参数错误");
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public Result<Void> handleOptimisticLockingFailureException(OptimisticLockingFailureException e) {
        log.warn("乐观锁冲突: {}", e.getMessage());
        return Result.badRequest("数据已被修改，请刷新后重试");
    }
}
