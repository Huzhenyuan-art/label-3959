package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.dto.OrderDetailDTO;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.mapper.OrderItemMapper;
import com.example.demo.mapper.OrderMapper;
import com.example.demo.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单 Service 实现
 * 演示：多表联查分页、事务 + 批量插入、乐观锁更新状态
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    @Override
    public IPage<OrderDetailDTO> pageOrders(int current, int size, String username, Integer status) {
        // 演示：XML 多表联查 + MyBatis Plus 分页插件
        Page<OrderDetailDTO> page = new Page<>(current, size);
        return orderMapper.selectOrderPage(page, username, status);
    }

    @Override
    public OrderDetailDTO getOrderDetail(Long id) {
        // 演示：XML 多表联查获取完整订单详情（含明细和商品）
        OrderDetailDTO detail = orderMapper.selectOrderDetail(id);
        if (detail == null) {
            throw new IllegalArgumentException("订单不存在: " + id);
        }
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(Order order, List<OrderItem> items) {
        // 演示：事务 + 计算总金额 + 批量插入明细
        BigDecimal total = items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(total);
        order.setStatus(0);
        order.setVersion(1);
        save(order);

        // 演示：批量插入订单明细
        items.forEach(item -> item.setOrderId(order.getId()));
        orderItemMapper.insert(items.get(0));
        if (items.size() > 1) {
            // saveBatch 批量插入
            for (int i = 1; i < items.size(); i++) {
                orderItemMapper.insert(items.get(i));
            }
        }
        log.info("创建订单成功: orderId={}, itemCount={}, total={}", order.getId(), items.size(), total);
        return order;
    }

    @Override
    public void updateOrderStatus(Long id, Integer status, Integer version) {
        // 演示：乐观锁更新 - 通过 version 防止并发修改
        Order order = new Order();
        order.setId(id);
        order.setStatus(status);
        order.setVersion(version);
        boolean success = updateById(order);
        if (!success) {
            throw new IllegalArgumentException("更新失败，订单状态已变更，请刷新后重试（乐观锁冲突）");
        }
        log.info("更新订单状态: id={}, status={}", id, status);
    }
}
