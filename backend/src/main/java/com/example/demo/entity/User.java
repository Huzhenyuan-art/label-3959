package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体
 * 演示特性：逻辑删除、乐观锁、自动填充
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String email;

    private Integer age;

    /** 密码（加密存储） */
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** 角色: ADMIN-管理员 USER-普通用户 */
    private String role;

    /** 状态: 0-禁用 1-启用 */
    private Integer status;

    /** 逻辑删除字段：0-未删除 1-已删除 */
    @TableLogic
    private Integer deleted;

    /** 乐观锁版本号 */
    @Version
    private Integer version;

    /** 自动填充 - 插入时赋值 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /** 自动填充 - 插入/更新时赋值 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
