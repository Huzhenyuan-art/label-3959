package com.example.demo.aspect;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.annotation.OperationLog;
import com.example.demo.entity.OperationLog;
import com.example.demo.enums.OperationTypeEnum;
import com.example.demo.service.OperationLogService;
import com.example.demo.util.SecurityUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;
    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(operationLogAnnotation)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLogAnnotation) throws Throwable {
        long startTime = System.currentTimeMillis();
        OperationTypeEnum operationType = operationLogAnnotation.type();
        Object result = null;
        Throwable throwable = null;

        Object beforeData = null;
        Object targetId = null;

        try {
            targetId = resolveTargetId(joinPoint, operationLogAnnotation.targetIdExpression(), null);
            beforeData = getBeforeData(operationLogAnnotation.targetType(), targetId, operationType);
        } catch (Exception e) {
            log.debug("执行前获取目标ID或变更前数据失败，将在执行后重试", e);
        }

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            throwable = t;
            throw t;
        } finally {
            try {
                if (targetId == null) {
                    targetId = resolveTargetId(joinPoint, operationLogAnnotation.targetIdExpression(), result);
                }
                long duration = System.currentTimeMillis() - startTime;
                saveOperationLog(joinPoint, operationLogAnnotation, operationType,
                        beforeData, result, targetId, throwable, duration);
            } catch (Exception e) {
                log.error("保存审计日志失败", e);
            }
        }
    }

    private Object resolveTargetId(ProceedingJoinPoint joinPoint, String expression, Object result) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = nameDiscoverer.getParameterNames(signature.getMethod());
        Object[] args = joinPoint.getArgs();

        EvaluationContext context = new StandardEvaluationContext();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }
        if (result != null) {
            context.setVariable("result", result);
        }

        Expression exp = parser.parseExpression(expression);
        return exp.getValue(context);
    }

    private Object getBeforeData(String targetType, Object targetId, OperationTypeEnum operationType) {
        if (targetId == null) {
            return null;
        }

        if (operationType.name().endsWith("_CREATE")) {
            return null;
        }

        try {
            String className = Character.toUpperCase(targetType.charAt(0)) + targetType.substring(1);
            String mapperName = className + "Mapper";
            String beanName = Character.toLowerCase(mapperName.charAt(0)) + mapperName.substring(1);

            Object mapperBean = applicationContext.getBean(beanName);
            if (mapperBean instanceof BaseMapper) {
                BaseMapper<?> mapper = (BaseMapper<?>) mapperBean;
                Long id = targetId instanceof Number ? ((Number) targetId).longValue() : Long.parseLong(targetId.toString());
                return mapper.selectById(id);
            }
        } catch (Exception e) {
            log.debug("获取变更前数据失败，targetType={}, targetId={}", targetType, targetId, e);
        }
        return null;
    }

    private void saveOperationLog(ProceedingJoinPoint joinPoint, OperationLog operationLogAnnotation,
                                  OperationTypeEnum operationType, Object beforeData, Object result,
                                  Object targetId, Throwable throwable, long duration) {
        OperationLog operationLog = new OperationLog();

        operationLog.setOperationType(operationType.getCode());
        operationLog.setOperationCategory(operationType.getCategory());
        operationLog.setOperationDesc(operationType.getDesc());

        operationLog.setOperatorId(SecurityUtil.getCurrentUserId());
        operationLog.setOperatorName(SecurityUtil.getCurrentUsername());
        operationLog.setOperatorRole(SecurityUtil.getCurrentRole());

        if (targetId != null) {
            operationLog.setTargetId(targetId instanceof Number ? ((Number) targetId).longValue() : Long.parseLong(targetId.toString()));
        }
        operationLog.setTargetType(operationLogAnnotation.targetType());

        operationLog.setBeforeData(beforeData);
        if (operationLogAnnotation.recordResult() && result != null && throwable == null) {
            operationLog.setAfterData(result);
        }

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            operationLog.setRequestMethod(request.getMethod());
            operationLog.setRequestUri(request.getRequestURI());
            operationLog.setIpAddress(getClientIp(request));
            operationLog.setUserAgent(request.getHeader("User-Agent"));

            if (operationLogAnnotation.recordParams()) {
                operationLog.setRequestParams(getRequestParams(joinPoint, request));
            }
        }

        operationLog.setStatus(throwable == null ? 1 : 0);
        if (throwable != null) {
            operationLog.setErrorMessage(throwable.getMessage());
        }
        operationLog.setDuration(duration);
        operationLog.setOperationTime(LocalDateTime.now());

        operationLogService.saveLogAsync(operationLog);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String getRequestParams(ProceedingJoinPoint joinPoint, HttpServletRequest request) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();

            Map<String, Object> params = new HashMap<>();
            for (int i = 0; i < paramNames.length; i++) {
                Object arg = args[i];
                if (arg != null && !isExcludedType(arg)) {
                    params.put(paramNames[i], arg);
                }
            }

            request.getParameterMap().forEach((key, values) -> {
                if (values != null && values.length > 0) {
                    params.put(key, values.length == 1 ? values[0] : values);
                }
            });

            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isExcludedType(Object obj) {
        return obj instanceof jakarta.servlet.ServletRequest
                || obj instanceof jakarta.servlet.ServletResponse
                || obj instanceof jakarta.servlet.http.HttpSession
                || obj instanceof java.io.InputStream
                || obj instanceof java.io.OutputStream;
    }
}
