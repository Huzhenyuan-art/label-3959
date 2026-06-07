package com.example.demo.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class RefundAuditDTO {

    @NotNull(message = "退款单ID不能为空")
    private Long refundId;

    @NotNull(message = "审核结果不能为空")
    private Boolean approved;

    private String auditRemark;
}
