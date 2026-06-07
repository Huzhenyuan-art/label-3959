package com.example.demo.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情 DTO（多表联查结果）
 */
@Data
public class OrderDetailDTO {
    private Long id;
    private Long userId;
    private String username;
    private String userEmail;
    private BigDecimal totalAmount;
    private Integer status;
    private String statusLabel;
    private String remark;
    private Long couponId;
    private BigDecimal discountAmount;
    private String couponName;
    private Long addressId;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private Integer version;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private List<OrderItemDTO> items;

    @Data
    public static class OrderItemDTO {
        private Long id;
        private Long productId;
        private String productName;
        private String productCategory;
        private Integer quantity;
        private BigDecimal price;
        private BigDecimal subtotal;
    }
}
