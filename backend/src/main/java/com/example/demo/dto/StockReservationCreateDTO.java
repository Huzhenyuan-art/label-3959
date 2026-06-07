package com.example.demo.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class StockReservationCreateDTO {

    private Long orderId;

    private List<ReservationItem> items;

    private LocalDateTime expireTime;

    @Data
    public static class ReservationItem {
        private Long orderItemId;
        private Long productId;
        private String productName;
        private Integer quantity;
    }
}
