package com.example.demo.enums;

import lombok.Getter;

@Getter
public enum OrderStatusEnum {

    PENDING_PAYMENT(0, "待支付"),
    PAID(1, "已支付"),
    SHIPPED(2, "已发货"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消"),
    REFUNDING(5, "退款中");

    private final Integer code;
    private final String desc;

    OrderStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDesc(Integer code) {
        for (OrderStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e.getDesc();
            }
        }
        return "未知";
    }

    public static OrderStatusEnum getByCode(Integer code) {
        for (OrderStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
