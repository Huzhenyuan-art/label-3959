package com.example.demo.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车项DTO（包含商品信息）
 */
@Data
public class CartItemDTO {

    private Long id;

    private Long userId;

    private Long productId;

    private String productName;

    private BigDecimal productPrice;

    private Integer productStock;

    private Integer productReservedStock;

    private String productCategory;

    private Integer quantity;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
