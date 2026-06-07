package com.example.demo.enums;

import lombok.Getter;

@Getter
public enum RefundStatusEnum {

    PENDING(0, "待审核"),
    APPROVED(1, "审核通过"),
    REJECTED(2, "审核拒绝"),
    CANCELLED(3, "已取消");

    private final Integer code;
    private final String desc;

    RefundStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static RefundStatusEnum getByCode(Integer code) {
        for (RefundStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
