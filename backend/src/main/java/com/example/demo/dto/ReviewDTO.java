package com.example.demo.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ReviewDTO {

    private Long id;

    private Long userId;

    private String username;

    private Long productId;

    private String productName;

    private Long orderItemId;

    private Long orderId;

    private Integer rating;

    private String content;

    private LocalDateTime createdTime;
}
