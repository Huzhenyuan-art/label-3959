package com.example.demo.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserCouponDTO {

    private Long id;

    private Long userId;

    private Long templateId;

    private String couponName;

    private Integer couponType;

    private String couponTypeDesc;

    private BigDecimal discountAmount;

    private BigDecimal discountRate;

    private BigDecimal minAmount;

    private String couponCode;

    private Integer status;

    private String statusDesc;

    private LocalDateTime usedTime;

    private Long orderId;

    private LocalDateTime validStartTime;

    private LocalDateTime validEndTime;

    private LocalDateTime createdTime;
}
