package com.example.demo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StockReservationDTO {

    private Long id;

    private Long orderId;

    private Long orderItemId;

    private Long productId;

    private String productName;

    private Integer quantity;

    private Integer status;

    private String statusDesc;

    private LocalDateTime expireTime;

    private String releaseReason;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
