package com.example.demo.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CouponUseResultDTO {

    private Long userCouponId;

    private Long templateId;

    private String couponName;

    private Integer couponType;

    private BigDecimal discountAmount;

    private BigDecimal originalAmount;

    private BigDecimal finalAmount;
}
