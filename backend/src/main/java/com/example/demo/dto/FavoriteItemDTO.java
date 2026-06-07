package com.example.demo.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收藏项DTO（包含商品信息）
 */
@Data
public class FavoriteItemDTO {

    private Long id;

    private Long userId;

    private Long productId;

    private String productName;

    private BigDecimal productPrice;

    private Integer productStock;

    private Integer productReservedStock;

    private String productCategory;

    private String productDescription;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

    public Integer getAvailableStock() {
        int total = productStock == null ? 0 : productStock;
        int reserved = productReservedStock == null ? 0 : productReservedStock;
        return Math.max(0, total - reserved);
    }

    public boolean getCanPurchase() {
        return getAvailableStock() > 0;
    }
}
