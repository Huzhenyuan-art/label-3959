package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体
 * 演示特性：乐观锁更新状态、多表关联查询
 */
@Data
@TableName("`order`")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private BigDecimal totalAmount;

    /** 订单状态: 0-待支付 1-已支付 2-已发货 3-已完成 4-已取消 */
    private Integer status;

    private String remark;

    private Long couponId;

    private BigDecimal discountAmount;

    private Long addressId;

    private String receiverName;

    private String receiverPhone;

    private String receiverAddress;

    /** 乐观锁版本号 */
    @Version
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
