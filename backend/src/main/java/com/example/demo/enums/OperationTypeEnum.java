package com.example.demo.enums;

import lombok.Getter;

@Getter
public enum OperationTypeEnum {

    USER_CREATE("USER_CREATE", "创建用户", "用户管理"),
    USER_UPDATE("USER_UPDATE", "更新用户", "用户管理"),
    USER_DELETE("USER_DELETE", "删除用户", "用户管理"),
    USER_RESTORE("USER_RESTORE", "恢复用户", "用户管理"),

    PRODUCT_CREATE("PRODUCT_CREATE", "创建商品", "商品管理"),
    PRODUCT_UPDATE("PRODUCT_UPDATE", "更新商品", "商品管理"),
    PRODUCT_DELETE("PRODUCT_DELETE", "删除商品", "商品管理"),

    ORDER_CREATE("ORDER_CREATE", "创建订单", "订单管理"),
    ORDER_UPDATE("ORDER_UPDATE", "更新订单", "订单管理"),
    ORDER_DELETE("ORDER_DELETE", "删除订单", "订单管理"),

    COUPON_CREATE("COUPON_CREATE", "创建优惠券", "优惠券管理"),
    COUPON_UPDATE("COUPON_UPDATE", "更新优惠券", "优惠券管理"),
    COUPON_DELETE("COUPON_DELETE", "删除优惠券", "优惠券管理"),

    REFUND_AUDIT("REFUND_AUDIT", "审核退款", "退款管理"),

    NOTIFICATION_SEND("NOTIFICATION_SEND", "发送通知", "消息管理");

    private final String code;
    private final String desc;
    private final String category;

    OperationTypeEnum(String code, String desc, String category) {
        this.code = code;
        this.desc = desc;
        this.category = category;
    }
}
