package com.example.demo.enums;

import lombok.Getter;

@Getter
public enum UserCouponStatusEnum {

    AVAILABLE(0, "未使用"),
    USED(1, "已使用"),
    EXPIRED(2, "已过期"),
    INVALID(3, "已失效");

    private final Integer code;
    private final String desc;

    UserCouponStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static UserCouponStatusEnum getByCode(Integer code) {
        for (UserCouponStatusEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
