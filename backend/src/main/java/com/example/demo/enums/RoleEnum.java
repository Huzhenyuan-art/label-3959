package com.example.demo.enums;

import lombok.Getter;

@Getter
public enum RoleEnum {

    ADMIN("ADMIN", "管理员"),
    USER("USER", "普通用户");

    private final String code;
    private final String desc;

    RoleEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
