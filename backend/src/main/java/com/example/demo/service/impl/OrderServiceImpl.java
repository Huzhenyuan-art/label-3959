package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.dto.CouponUseResultDTO;
import com.example.demo.dto.OrderDetailDTO;
import com.example.demo.dto.StockReservationCreateDTO;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.UserAddress;
import com.example.demo.enums.OrderStatusEnum;
import com.example.demo.mapper.OrderItemMapper;
import com.example.demo.mapper.OrderMapper;
import com.example.demo.mapper.UserAddressMapper;
import com.example.demo.service.CouponService;
import com.example.demo.service.NotificationService;
import com.example.demo.service.OrderService;
import com.example.demo.service.StockReservationService;
import com.example.demo.util.AddressUtil;
import com.example.demo.util.OrderItemBatchUtil;
import com.example.demo.util.OrderValidationUtil;
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
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);
    private static final int RESERVATION_EXPIRE_MINUTES = 30;

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final NotificationService notificationService;
    private final CouponService couponService;
    private final UserAddressMapper userAddressMapper;
    private final StockReservationService stockReservationService;
    private final OrderItemBatchUtil orderItemBatchUtil;

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
        SecurityUtil.checkResourceOwnerOrThrow(detail.getUserId());
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(Order order, List<OrderItem> items, Long userCouponId, Long addressId) {
        BigDecimal originalTotal = calculateOriginalTotal(items);
        BigDecimal discountAmount = applyCoupon(order, userCouponId, originalTotal);
        UserAddress address = resolveAddress(addressId);
        populateOrderAddress(order, address);

        order.setTotalAmount(originalTotal.subtract(discountAmount).max(BigDecimal.ZERO));
        order.setStatus(OrderStatusEnum.PENDING_PAYMENT.getCode());
        order.setVersion(1);
        if (!SecurityUtil.isAdmin()) {
            order.setUserId(SecurityUtil.getCurrentUserId());
        }
        save(order);

        if (userCouponId != null) {
            couponService.useCoupon(userCouponId, order.getId(), originalTotal);
        }

        items.forEach(item -> item.setOrderId(order.getId()));
        orderItemBatchUtil.batchInsert(items);

        createStockReservations(order.getId(), items);

        logger.info("创建订单成功: orderId={}, itemCount={}, originalTotal={}, discountAmount={}, finalTotal={}",
                order.getId(), items.size(), originalTotal, discountAmount, order.getTotalAmount());
        return order;
    }

    @Override
    public void updateOrderStatus(Long id, Integer status, Integer version) {
        Order existingOrder = OrderValidationUtil.getOrderAndCheckPermission(orderMapper, id);
        OrderValidationUtil.validateVersionMatch(existingOrder, version);

        Integer oldStatus = existingOrder.getStatus();
        Order order = new Order();
        order.setId(id);
        order.setStatus(status);
        order.setVersion(version);

        updateOrderWithRetry(order, update -> {
            if (!oldStatus.equals(status)) {
                handleStatusChange(id, existingOrder, oldStatus, status);
            }
        });

        logger.info("更新订单状态: id={}, oldStatus={}, newStatus={}", id, oldStatus, status);
    }

    @Override
    public void processRefund(Long id, boolean success, String reason) {
        Order existingOrder = OrderValidationUtil.getOrderOrThrow(orderMapper, id);
        SecurityUtil.checkAdminOrThrow();
        notificationService.sendRefundResultNotification(existingOrder.getUserId(), id, success, reason);
        logger.info("处理订单退款: id={}, success={}, reason={}", id, success, reason);
    }

    @Override
    public void updateRemark(Long id, String remark, Integer version) {
        Order existingOrder = OrderValidationUtil.getOrderAndCheckPermission(orderMapper, id);
        OrderValidationUtil.validateVersionMatch(existingOrder, version);

        Order order = new Order();
        order.setId(id);
        order.setRemark(remark);
        order.setVersion(version);

        updateOrderWithRetry(order, update ->
                logger.info("更新订单备注: id={}, oldRemark={}, newRemark={}", id, existingOrder.getRemark(), remark));
    }

    private BigDecimal calculateOriginalTotal(List<OrderItem> items) {
        return items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal applyCoupon(Order order, Long userCouponId, BigDecimal originalTotal) {
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
        return discountAmount;
    }

    private UserAddress resolveAddress(Long addressId) {
        Long userId = SecurityUtil.getCurrentUserId();
        UserAddress address;
        if (addressId != null) {
            address = userAddressMapper.selectById(addressId);
            if (address == null) {
                throw new IllegalArgumentException("收货地址不存在");
            }
            SecurityUtil.checkResourceOwnerOrThrow(address.getUserId());
        } else {
            address = userAddressMapper.selectOne(
                    new LambdaQueryWrapper<UserAddress>()
                            .eq(UserAddress::getUserId, userId)
                            .eq(UserAddress::getIsDefault, 1)
            );
        }
        if (address == null) {
            throw new IllegalArgumentException("请先添加收货地址");
        }
        return address;
    }

    private void populateOrderAddress(Order order, UserAddress address) {
        order.setAddressId(address.getId());
        order.setReceiverName(address.getReceiverName());
        order.setReceiverPhone(address.getReceiverPhone());
        order.setReceiverAddress(AddressUtil.buildFullAddress(address));
    }

    private void createStockReservations(Long orderId, List<OrderItem> items) {
        StockReservationCreateDTO reservationDTO = new StockReservationCreateDTO();
        reservationDTO.setOrderId(orderId);
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
    }

    private void updateOrderWithRetry(Order order, Consumer<Order> onSuccess) {
        boolean success = updateById(order);
        if (!success) {
            throw new IllegalArgumentException("更新失败，订单数据已变更，请刷新后重试（乐观锁冲突）");
        }
        onSuccess.accept(order);
    }

    private void handleStatusChange(Long orderId, Order existingOrder, Integer oldStatus, Integer newStatus) {
        notificationService.sendOrderStatusNotification(existingOrder.getUserId(), orderId, oldStatus, newStatus);

        if (OrderStatusEnum.CANCELLED.getCode().equals(newStatus)) {
            if (existingOrder.getCouponId() != null) {
                couponService.restoreCoupon(orderId);
                logger.info("订单已取消，恢复优惠券: orderId={}, userCouponId={}", orderId, existingOrder.getCouponId());
            }
            stockReservationService.releaseReservations(orderId, "订单已取消");
            logger.info("订单已取消，释放库存预占: orderId={}", orderId);
        }

        if (OrderStatusEnum.COMPLETED.getCode().equals(newStatus)) {
            stockReservationService.deductStock(orderId);
            logger.info("订单已完成，正式扣减库存: orderId={}", orderId);
        }
    }
}
