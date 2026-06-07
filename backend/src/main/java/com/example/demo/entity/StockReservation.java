package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("stock_reservation")
public class StockReservation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long orderItemId;

    private Long productId;

    private String productName;

    private Integer quantity;

    private Integer status;

    private LocalDateTime expireTime;

    private String releaseReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
