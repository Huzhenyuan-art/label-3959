package com.example.demo.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PendingReviewDTO {

    private Long orderItemId;

    private Long orderId;

    private Long productId;

    private String productName;

    private Integer quantity;

    private BigDecimal price;

    private LocalDateTime orderCreatedTime;
}
