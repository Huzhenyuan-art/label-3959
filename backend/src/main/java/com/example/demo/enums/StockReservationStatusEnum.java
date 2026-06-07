package com.example.demo.enums;

import lombok.Getter;

@Getter
public enum StockReservationStatusEnum {

    RESERVED(0, "预占中"),
    RELEASED(1, "已释放"),
    DEDUCTED(2, "已扣减");

    private final Integer code;
    private final String desc;

    StockReservationStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDesc(Integer code) {
        for (StockReservationStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e.getDesc();
            }
        }
        return "未知";
    }
}
