package com.example.demo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.dto.OrderDetailDTO;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;

import java.util.List;

public interface OrderService extends IService<Order> {

    IPage<OrderDetailDTO> pageOrders(int current, int size, String username, Integer status);

    OrderDetailDTO getOrderDetail(Long id);

    Order createOrder(Order order, List<OrderItem> items, Long userCouponId);

    void updateOrderStatus(Long id, Integer status, Integer version);

    void processRefund(Long id, boolean success, String reason);
}
