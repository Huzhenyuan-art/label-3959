package com.example.demo.util;

import com.example.demo.entity.Order;
import com.example.demo.mapper.OrderMapper;

public final class OrderValidationUtil {

    private OrderValidationUtil() {
    }

    public static Order getOrderOrThrow(OrderMapper orderMapper, Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在: " + id);
        }
        return order;
    }

    public static Order getOrderAndCheckPermission(OrderMapper orderMapper, Long id) {
        Order order = getOrderOrThrow(orderMapper, id);
        SecurityUtil.checkResourceOwnerOrThrow(order.getUserId());
        return order;
    }

    public static void validateRefundableStatus(Integer status) {
        if (status != 1 && status != 2 && status != 3) {
            throw new IllegalArgumentException("当前订单状态不允许申请退款");
        }
    }

    public static void validateVersionMatch(Order existing, Integer version) {
        if (!existing.getVersion().equals(version)) {
            throw new IllegalArgumentException("更新失败，订单数据已变更，请刷新后重试（乐观锁冲突）");
        }
    }
}
