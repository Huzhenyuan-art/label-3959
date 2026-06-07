package com.example.demo.enums;

import lombok.Getter;

@Getter
public enum CouponTemplateStatusEnum {

    DRAFT(0, "草稿"),
    ACTIVE(1, "进行中"),
    EXPIRED(2, "已过期"),
    DISABLED(3, "已停用");

    private final Integer code;
    private final String desc;

    CouponTemplateStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static CouponTemplateStatusEnum getByCode(Integer code) {
        for (CouponTemplateStatusEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
