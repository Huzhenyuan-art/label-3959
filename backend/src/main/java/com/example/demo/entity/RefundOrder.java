package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("refund_order")
public class RefundOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long userId;

    private String refundNo;

    private BigDecimal refundAmount;

    private Integer refundType;

    private String refundReason;

    private String refundDesc;

    private String proofImages;

    private Integer status;

    private Integer originalOrderStatus;

    private String auditRemark;

    private Long auditUserId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    private LocalDateTime auditTime;
}
