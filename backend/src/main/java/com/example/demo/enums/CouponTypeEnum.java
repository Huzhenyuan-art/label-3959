package com.example.demo.enums;

import lombok.Getter;

@Getter
public enum CouponTypeEnum {

    FIXED_AMOUNT(1, "满减券"),
    DISCOUNT_RATE(2, "折扣券");

    private final Integer code;
    private final String desc;

    CouponTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static CouponTypeEnum getByCode(Integer code) {
        for (CouponTypeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
