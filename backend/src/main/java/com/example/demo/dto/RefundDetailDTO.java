package com.example.demo.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RefundDetailDTO {

    private Long id;

    private Long orderId;

    private Long userId;

    private String username;

    private String refundNo;

    private BigDecimal refundAmount;

    private Integer refundType;

    private String refundTypeLabel;

    private String refundReason;

    private String refundDesc;

    private String proofImages;

    private Integer status;

    private String statusLabel;

    private String auditRemark;

    private Long auditUserId;

    private String auditUsername;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

    private LocalDateTime auditTime;

    private BigDecimal orderTotalAmount;

    private Integer orderStatus;

    private String orderStatusLabel;
}
