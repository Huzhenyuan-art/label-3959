package com.example.demo.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class RefundApplyDTO {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @NotNull(message = "退款金额不能为空")
    private BigDecimal refundAmount;

    @NotNull(message = "退款类型不能为空")
    private Integer refundType;

    @NotBlank(message = "退款原因不能为空")
    private String refundReason;

    private String refundDesc;

    private String proofImages;
}
