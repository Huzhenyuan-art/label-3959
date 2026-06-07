package com.example.demo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationLogQueryDTO {

    private String operationType;

    private String operationCategory;

    private String operatorName;

    private Long targetId;

    private String targetType;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer status;
}
