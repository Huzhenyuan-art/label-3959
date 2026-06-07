package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.dto.RefundApplyDTO;
import com.example.demo.dto.RefundAuditDTO;
import com.example.demo.dto.RefundDetailDTO;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.Product;
import com.example.demo.entity.RefundOrder;
import com.example.demo.enums.RefundStatusEnum;
import com.example.demo.mapper.OrderItemMapper;
import com.example.demo.mapper.OrderMapper;
import com.example.demo.mapper.ProductMapper;
import com.example.demo.mapper.RefundOrderMapper;
import com.example.demo.service.NotificationService;
import com.example.demo.service.RefundOrderService;
import com.example.demo.service.StockReservationService;
import com.example.demo.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundOrderServiceImpl extends ServiceImpl<RefundOrderMapper, RefundOrder> implements RefundOrderService {

    private final RefundOrderMapper refundOrderMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final NotificationService notificationService;
    private final StockReservationService stockReservationService;

    @Override
    public IPage<RefundDetailDTO> pageRefunds(int current, int size, String refundNo, Long orderId, String username, Integer status) {
        Long currentUserId = SecurityUtil.isAdmin() ? null : SecurityUtil.getCurrentUserId();
        Page<RefundDetailDTO> page = new Page<>(current, size);
        return refundOrderMapper.selectRefundPage(page, refundNo, orderId, username, status, currentUserId);
    }

    @Override
    public RefundDetailDTO getRefundDetail(Long id) {
        RefundDetailDTO detail = refundOrderMapper.selectRefundDetail(id);
        if (detail == null) {
            throw new IllegalArgumentException("退款单不存在: " + id);
        }
        if (!SecurityUtil.isAdmin() && !detail.getUserId().equals(SecurityUtil.getCurrentUserId())) {
            throw new SecurityException("无权查看他人退款单");
        }
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundOrder applyRefund(RefundApplyDTO dto) {
        Order order = orderMapper.selectById(dto.getOrderId());
        if (order == null) {
            throw new IllegalArgumentException("订单不存在: " + dto.getOrderId());
        }

        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (!order.getUserId().equals(currentUserId)) {
            throw new SecurityException("无权申请他人订单退款");
        }

        if (order.getStatus() != 1 && order.getStatus() != 2 && order.getStatus() != 3) {
            throw new IllegalArgumentException("当前订单状态不允许申请退款");
        }

        RefundOrder existingPending = refundOrderMapper.selectPendingByOrderId(dto.getOrderId());
        if (existingPending != null) {
            throw new IllegalArgumentException("该订单已有待审核的退款申请");
        }

        if (dto.getRefundAmount().compareTo(order.getTotalAmount()) > 0) {
            throw new IllegalArgumentException("退款金额不能超过订单金额");
        }

        RefundOrder refundOrder = new RefundOrder();
        refundOrder.setOrderId(dto.getOrderId());
        refundOrder.setUserId(currentUserId);
        String refundNo = generateRefundNo();
        refundOrder.setRefundNo(refundNo);
        refundOrder.setRefundAmount(dto.getRefundAmount());
        refundOrder.setRefundType(dto.getRefundType());
        refundOrder.setRefundReason(dto.getRefundReason());
        refundOrder.setRefundDesc(dto.getRefundDesc());
        refundOrder.setProofImages(dto.getProofImages());
        refundOrder.setStatus(RefundStatusEnum.PENDING.getCode());
        refundOrder.setOriginalOrderStatus(order.getStatus());
        save(refundOrder);

        Integer oldOrderStatus = order.getStatus();
        order.setStatus(5);
        orderMapper.updateById(order);
        notificationService.sendOrderStatusNotification(currentUserId, dto.getOrderId(), oldOrderStatus, 5);

        notificationService.sendRefundApplyNotification(currentUserId, dto.getOrderId(), refundNo);

        log.info("用户申请退款成功: refundId={}, orderId={}, userId={}, amount={}, refundNo={}",
                refundOrder.getId(), dto.getOrderId(), currentUserId, dto.getRefundAmount(), refundNo);

        return refundOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditRefund(RefundAuditDTO dto) {
        if (!SecurityUtil.isAdmin()) {
            throw new SecurityException("只有管理员可以审核退款");
        }

        RefundOrder refundOrder = getById(dto.getRefundId());
        if (refundOrder == null) {
            throw new IllegalArgumentException("退款单不存在: " + dto.getRefundId());
        }

        if (!RefundStatusEnum.PENDING.getCode().equals(refundOrder.getStatus())) {
            throw new IllegalArgumentException("退款单状态不允许审核");
        }

        Order order = orderMapper.selectById(refundOrder.getOrderId());
        if (order == null) {
            throw new IllegalArgumentException("关联订单不存在");
        }

        refundOrder.setAuditUserId(SecurityUtil.getCurrentUserId());
        refundOrder.setAuditRemark(dto.getAuditRemark());
        refundOrder.setAuditTime(LocalDateTime.now());

        if (dto.getApproved()) {
            refundOrder.setStatus(RefundStatusEnum.APPROVED.getCode());

            Integer oldStatus = order.getStatus();
            order.setStatus(4);
            orderMapper.updateById(order);

            rollbackStock(refundOrder.getOrderId());

            stockReservationService.releaseReservations(order.getId(), "退款审核通过，库存回滚");

            notificationService.sendOrderStatusNotification(order.getUserId(), order.getId(), oldStatus, 4);

            log.info("退款审核通过: refundId={}, orderId={}, userId={}",
                    dto.getRefundId(), refundOrder.getOrderId(), refundOrder.getUserId());
        } else {
            refundOrder.setStatus(RefundStatusEnum.REJECTED.getCode());

            Integer recoverStatus = refundOrder.getOriginalOrderStatus();
            if (recoverStatus != null) {
                Integer oldStatus = order.getStatus();
                order.setStatus(recoverStatus);
                orderMapper.updateById(order);
                notificationService.sendOrderStatusNotification(order.getUserId(), order.getId(), oldStatus, recoverStatus);
            }

            log.info("退款审核拒绝: refundId={}, orderId={}, userId={}, reason={}",
                    dto.getRefundId(), refundOrder.getOrderId(), refundOrder.getUserId(), dto.getAuditRemark());
        }

        updateById(refundOrder);

        notificationService.sendRefundResultNotification(
                refundOrder.getUserId(),
                refundOrder.getOrderId(),
                dto.getApproved(),
                dto.getAuditRemark()
        );
    }

    @Override
    public void cancelRefund(Long id) {
        RefundOrder refundOrder = getById(id);
        if (refundOrder == null) {
            throw new IllegalArgumentException("退款单不存在: " + id);
        }

        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (!refundOrder.getUserId().equals(currentUserId)) {
            throw new SecurityException("无权取消他人退款申请");
        }

        if (!RefundStatusEnum.PENDING.getCode().equals(refundOrder.getStatus())) {
            throw new IllegalArgumentException("退款单状态不允许取消");
        }

        refundOrder.setStatus(RefundStatusEnum.CANCELLED.getCode());
        updateById(refundOrder);

        Order order = orderMapper.selectById(refundOrder.getOrderId());
        if (order != null && refundOrder.getOriginalOrderStatus() != null) {
            Integer oldStatus = order.getStatus();
            order.setStatus(refundOrder.getOriginalOrderStatus());
            orderMapper.updateById(order);
            notificationService.sendOrderStatusNotification(currentUserId, order.getId(), oldStatus, refundOrder.getOriginalOrderStatus());
        }

        log.info("用户取消退款申请: refundId={}, orderId={}", id, refundOrder.getOrderId());
    }

    private void rollbackStock(Long orderId) {
        List<OrderItem> orderItems = orderItemMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, orderId)
        );

        for (OrderItem item : orderItems) {
            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                product.setStock(product.getStock() + item.getQuantity());
                productMapper.updateById(product);
                log.info("库存回滚: productId={}, productName={}, quantity={}",
                        item.getProductId(), item.getProductName(), item.getQuantity());
            }
        }
    }

    private String generateRefundNo() {
        return "REF" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
