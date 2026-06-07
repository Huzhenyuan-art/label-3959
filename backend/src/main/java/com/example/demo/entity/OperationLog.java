package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "operation_log", autoResultMap = true)
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String operationType;

    private String operationCategory;

    private String operationDesc;

    private Long operatorId;

    private String operatorName;

    private String operatorRole;

    private Long targetId;

    private String targetType;

    @TableField(value = "before_data", typeHandler = com.example.demo.handler.JsonTypeHandler.class)
    private Object beforeData;

    @TableField(value = "after_data", typeHandler = com.example.demo.handler.JsonTypeHandler.class)
    private Object afterData;

    private String requestMethod;

    private String requestUri;

    private String requestParams;

    private String ipAddress;

    private String userAgent;

    private Integer status;

    private String errorMessage;

    private Long duration;

    private LocalDateTime operationTime;
}
