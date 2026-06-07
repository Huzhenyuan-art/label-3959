package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.dto.CouponUseResultDTO;
import com.example.demo.dto.OrderDetailDTO;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.UserAddress;
import com.example.demo.mapper.OrderItemMapper;
import com.example.demo.mapper.OrderMapper;
import com.example.demo.mapper.UserAddressMapper;
import com.example.demo.dto.StockReservationCreateDTO;
import com.example.demo.service.CouponService;
import com.example.demo.service.NotificationService;
import com.example.demo.service.OrderService;
import com.example.demo.service.StockReservationService;
import com.example.demo.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final NotificationService notificationService;
    private final CouponService couponService;
    private final UserAddressMapper userAddressMapper;
    private final StockReservationService stockReservationService;

    private static final int RESERVATION_EXPIRE_MINUTES = 30;

    @Override
    public IPage<OrderDetailDTO> pageOrders(int current, int size, String username, Integer status, String createdTimeStart, String createdTimeEnd) {
        Long currentUserId = SecurityUtil.isAdmin() ? null : SecurityUtil.getCurrentUserId();
        Page<OrderDetailDTO> page = new Page<>(current, size);
        return orderMapper.selectOrderPage(page, username, status, currentUserId, createdTimeStart, createdTimeEnd);
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
    public Order createOrder(Order order, List<OrderItem> items, Long userCouponId, Long addressId) {
        BigDecimal originalTotal = items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountAmount = BigDecimal.ZERO;
        if (userCouponId != null) {
            CouponUseResultDTO discountResult = couponService.calculateDiscount(userCouponId, originalTotal);
            discountAmount = discountResult.getDiscountAmount();
            order.setCouponId(userCouponId);
            order.setDiscountAmount(discountAmount);
        } else {
            order.setCouponId(null);
            order.setDiscountAmount(BigDecimal.ZERO);
        }

        Long userId = SecurityUtil.getCurrentUserId();
        UserAddress address = null;
        if (addressId != null) {
            address = userAddressMapper.selectById(addressId);
            if (address == null) {
                throw new IllegalArgumentException("收货地址不存在");
            }
            if (!address.getUserId().equals(userId)) {
                throw new SecurityException("无权使用该地址");
            }
        } else {
            address = userAddressMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserAddress>()
                    .eq(UserAddress::getUserId, userId)
                    .eq(UserAddress::getIsDefault, 1)
            );
        }

        if (address != null) {
            order.setAddressId(address.getId());
            order.setReceiverName(address.getReceiverName());
            order.setReceiverPhone(address.getReceiverPhone());
            String fullAddress = buildFullAddress(address);
            order.setReceiverAddress(fullAddress);
        } else {
            throw new IllegalArgumentException("请先添加收货地址");
        }

        order.setTotalAmount(originalTotal.subtract(discountAmount).max(BigDecimal.ZERO));
        order.setStatus(0);
        order.setVersion(1);
        if (!SecurityUtil.isAdmin()) {
            order.setUserId(userId);
        }
        save(order);

        if (userCouponId != null) {
            couponService.useCoupon(userCouponId, order.getId(), originalTotal);
        }

        items.forEach(item -> item.setOrderId(order.getId()));
        orderItemMapper.insert(items.get(0));
        if (items.size() > 1) {
            for (int i = 1; i < items.size(); i++) {
                orderItemMapper.insert(items.get(i));
            }
        }

        StockReservationCreateDTO reservationDTO = new StockReservationCreateDTO();
        reservationDTO.setOrderId(order.getId());
        reservationDTO.setExpireTime(LocalDateTime.now().plusMinutes(RESERVATION_EXPIRE_MINUTES));
        List<StockReservationCreateDTO.ReservationItem> reservationItems = new ArrayList<>();
        for (OrderItem item : items) {
            StockReservationCreateDTO.ReservationItem ri = new StockReservationCreateDTO.ReservationItem();
            ri.setOrderItemId(item.getId());
            ri.setProductId(item.getProductId());
            ri.setProductName(item.getProductName());
            ri.setQuantity(item.getQuantity());
            reservationItems.add(ri);
        }
        reservationDTO.setItems(reservationItems);
        stockReservationService.createReservations(reservationDTO);

        logger.info("创建订单成功: orderId={}, itemCount={}, originalTotal={}, discountAmount={}, finalTotal={}",
                order.getId(), items.size(), originalTotal, discountAmount, order.getTotalAmount());
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
        logger.info("更新订单状态: id={}, oldStatus={}, newStatus={}", id, oldStatus, status);

        if (!oldStatus.equals(status)) {
            notificationService.sendOrderStatusNotification(existingOrder.getUserId(), id, oldStatus, status);

            if (status == 4 && existingOrder.getCouponId() != null) {
                couponService.restoreCoupon(id);
                logger.info("订单已取消，恢复优惠券: orderId={}, userCouponId={}", id, existingOrder.getCouponId());
            }

            if (status == 4) {
                stockReservationService.releaseReservations(id, "订单已取消");
                logger.info("订单已取消，释放库存预占: orderId={}", id);
            }

            if (status == 3) {
                stockReservationService.deductStock(id);
                logger.info("订单已完成，正式扣减库存: orderId={}", id);
            }
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
        logger.info("处理订单退款: id={}, success={}, reason={}", id, success, reason);
    }

    @Override
    public void updateRemark(Long id, String remark, Integer version) {
        Order existingOrder = getById(id);
        if (existingOrder == null) {
            throw new IllegalArgumentException("订单不存在: " + id);
        }
        if (!SecurityUtil.isAdmin() && !existingOrder.getUserId().equals(SecurityUtil.getCurrentUserId())) {
            throw new SecurityException("无权修改他人订单备注");
        }
        Order order = new Order();
        order.setId(id);
        order.setRemark(remark);
        order.setVersion(version);
        boolean success = updateById(order);
        if (!success) {
            throw new IllegalArgumentException("更新失败，订单数据已变更，请刷新后重试（乐观锁冲突）");
        }
        logger.info("更新订单备注: id={}, oldRemark={}, newRemark={}", id, existingOrder.getRemark(), remark);
    }

    private String buildFullAddress(UserAddress address) {
        StringBuilder sb = new StringBuilder();
        if (address.getProvince() != null && !address.getProvince().isEmpty()) {
            sb.append(address.getProvince());
        }
        if (address.getCity() != null && !address.getCity().isEmpty()) {
            sb.append(address.getCity());
        }
        if (address.getDistrict() != null && !address.getDistrict().isEmpty()) {
            sb.append(address.getDistrict());
        }
        sb.append(address.getDetailAddress());
        return sb.toString();
    }
}
