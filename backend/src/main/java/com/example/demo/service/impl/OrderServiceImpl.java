package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.dto.OrderDetailDTO;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.mapper.OrderItemMapper;
import com.example.demo.mapper.OrderMapper;
import com.example.demo.service.NotificationService;
import com.example.demo.service.OrderService;
import com.example.demo.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final NotificationService notificationService;

    @Override
    public IPage<OrderDetailDTO> pageOrders(int current, int size, String username, Integer status) {
        Long currentUserId = SecurityUtil.isAdmin() ? null : SecurityUtil.getCurrentUserId();
        Page<OrderDetailDTO> page = new Page<>(current, size);
        return orderMapper.selectOrderPage(page, username, status, currentUserId);
    }

    @Override
    public OrderDetailDTO getOrderDetail(Long id) {
        OrderDetailDTO detail = orderMapper.selectOrderDetail(id);
        if (detail == null) {
            throw new IllegalArgumentException("订单不存在: " + id);
        }
        if (!SecurityUtil.isAdmin() && !detail.getUserId().equals(SecurityUtil.getCurrentUserId())) {
            throw new SecurityException("无权查看他人订单");
        }
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(Order order, List<OrderItem> items) {
        BigDecimal total = items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(total);
        order.setStatus(0);
        order.setVersion(1);
        if (!SecurityUtil.isAdmin()) {
            order.setUserId(SecurityUtil.getCurrentUserId());
        }
        save(order);

        items.forEach(item -> item.setOrderId(order.getId()));
        orderItemMapper.insert(items.get(0));
        if (items.size() > 1) {
            for (int i = 1; i < items.size(); i++) {
                orderItemMapper.insert(items.get(i));
            }
        }
        log.info("创建订单成功: orderId={}, itemCount={}, total={}", order.getId(), items.size(), total);
        return order;
    }

    @Override
    public void updateOrderStatus(Long id, Integer status, Integer version) {
        Order existingOrder = getById(id);
        if (existingOrder == null) {
            throw new IllegalArgumentException("订单不存在: " + id);
        }
        if (!SecurityUtil.isAdmin() && !existingOrder.getUserId().equals(SecurityUtil.getCurrentUserId())) {
            throw new SecurityException("无权修改他人订单");
        }
        Integer oldStatus = existingOrder.getStatus();
        Order order = new Order();
        order.setId(id);
        order.setStatus(status);
        order.setVersion(version);
        boolean success = updateById(order);
        if (!success) {
            throw new IllegalArgumentException("更新失败，订单状态已变更，请刷新后重试（乐观锁冲突）");
        }
        log.info("更新订单状态: id={}, oldStatus={}, newStatus={}", id, oldStatus, status);

        if (!oldStatus.equals(status)) {
            notificationService.sendOrderStatusNotification(existingOrder.getUserId(), id, oldStatus, status);
        }
    }

    @Override
    public void processRefund(Long id, boolean success, String reason) {
        Order existingOrder = getById(id);
        if (existingOrder == null) {
            throw new IllegalArgumentException("订单不存在: " + id);
        }
        if (!SecurityUtil.isAdmin()) {
            throw new SecurityException("只有管理员可以处理退款");
        }
        notificationService.sendRefundResultNotification(existingOrder.getUserId(), id, success, reason);
        log.info("处理订单退款: id={}, success={}, reason={}", id, success, reason);
    }
}
