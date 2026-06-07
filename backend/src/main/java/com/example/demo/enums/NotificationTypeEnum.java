package com.example.demo.enums;

import lombok.Getter;

@Getter
public enum NotificationTypeEnum {

    ORDER_STATUS_CHANGE(1, "订单状态变更"),
    REFUND_RESULT(2, "退款处理结果"),
    STOCK_WARNING(3, "库存预警"),
    SYSTEM_NOTICE(4, "系统通知"),
    REFUND_APPLY(5, "退款申请提交");

    private final Integer code;
    private final String desc;

    NotificationTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static NotificationTypeEnum getByCode(Integer code) {
        for (NotificationTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
