package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.dto.RefundApplyDTO;
import com.example.demo.dto.RefundAuditDTO;
import com.example.demo.dto.RefundDetailDTO;
import com.example.demo.entity.Order;
import com.example.demo.entity.RefundOrder;
import com.example.demo.enums.OrderStatusEnum;
import com.example.demo.enums.RefundStatusEnum;
import com.example.demo.mapper.OrderItemMapper;
import com.example.demo.mapper.OrderMapper;
import com.example.demo.mapper.ProductMapper;
import com.example.demo.mapper.RefundOrderMapper;
import com.example.demo.service.NotificationService;
import com.example.demo.service.RefundOrderService;
import com.example.demo.service.StockReservationService;
import com.example.demo.util.OrderValidationUtil;
import com.example.demo.util.RefundValidationUtil;
import com.example.demo.util.SecurityUtil;
import com.example.demo.util.StockOperationUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RefundOrderServiceImpl extends ServiceImpl<RefundOrderMapper, RefundOrder> implements RefundOrderService {

    private static final Logger logger = LoggerFactory.getLogger(RefundOrderServiceImpl.class);

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
        SecurityUtil.checkResourceOwnerOrThrow(detail.getUserId());
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundOrder applyRefund(RefundApplyDTO dto) {
        Order order = OrderValidationUtil.getOrderOrThrow(orderMapper, dto.getOrderId());
        SecurityUtil.checkResourceOwnerOrThrow(order.getUserId());
        OrderValidationUtil.validateRefundableStatus(order.getStatus());
        RefundValidationUtil.checkNoPendingRefund(refundOrderMapper, dto.getOrderId());
        RefundValidationUtil.validateRefundAmount(dto.getRefundAmount(), order.getTotalAmount());

        RefundOrder refundOrder = buildRefundOrder(dto, order);
        save(refundOrder);

        updateOrderStatus(order, OrderStatusEnum.REFUNDING.getCode());

        notificationService.sendRefundApplyNotification(order.getUserId(), dto.getOrderId(), refundOrder.getRefundNo());

        logger.info("用户申请退款成功: refundId={}, orderId={}, userId={}, amount={}, refundNo={}",
                refundOrder.getId(), dto.getOrderId(), order.getUserId(), dto.getRefundAmount(), refundOrder.getRefundNo());

        return refundOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditRefund(RefundAuditDTO dto) {
        SecurityUtil.checkAdminOrThrow();

        RefundOrder refundOrder = RefundValidationUtil.getRefundOrThrow(refundOrderMapper, dto.getRefundId());
        RefundValidationUtil.checkPendingStatus(refundOrder);
        Order order = OrderValidationUtil.getOrderOrThrow(orderMapper, refundOrder.getOrderId());

        refundOrder.setAuditUserId(SecurityUtil.getCurrentUserId());
        refundOrder.setAuditRemark(dto.getAuditRemark());
        refundOrder.setAuditTime(LocalDateTime.now());

        if (dto.getApproved()) {
            handleApprovedRefund(refundOrder, order);
        } else {
            handleRejectedRefund(refundOrder, order);
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
    @Transactional(rollbackFor = Exception.class)
    public void cancelRefund(Long id) {
        RefundOrder refundOrder = RefundValidationUtil.getRefundAndCheckPermission(refundOrderMapper, id);
        RefundValidationUtil.checkPendingStatus(refundOrder);

        refundOrder.setStatus(RefundStatusEnum.CANCELLED.getCode());
        updateById(refundOrder);

        Order order = orderMapper.selectById(refundOrder.getOrderId());
        if (order != null && refundOrder.getOriginalOrderStatus() != null) {
            updateOrderStatus(order, refundOrder.getOriginalOrderStatus());
        }

        logger.info("用户取消退款申请: refundId={}, orderId={}", id, refundOrder.getOrderId());
    }

    private RefundOrder buildRefundOrder(RefundApplyDTO dto, Order order) {
        RefundOrder refundOrder = new RefundOrder();
        refundOrder.setOrderId(dto.getOrderId());
        refundOrder.setUserId(order.getUserId());
        refundOrder.setRefundNo(RefundValidationUtil.generateRefundNo());
        refundOrder.setRefundAmount(dto.getRefundAmount());
        refundOrder.setRefundType(dto.getRefundType());
        refundOrder.setRefundReason(dto.getRefundReason());
        refundOrder.setRefundDesc(dto.getRefundDesc());
        refundOrder.setProofImages(dto.getProofImages());
        refundOrder.setStatus(RefundStatusEnum.PENDING.getCode());
        refundOrder.setOriginalOrderStatus(order.getStatus());
        return refundOrder;
    }

    private void updateOrderStatus(Order order, Integer newStatus) {
        Integer oldStatus = order.getStatus();
        order.setStatus(newStatus);
        orderMapper.updateById(order);
        notificationService.sendOrderStatusNotification(order.getUserId(), order.getId(), oldStatus, newStatus);
    }

    private void handleApprovedRefund(RefundOrder refundOrder, Order order) {
        refundOrder.setStatus(RefundStatusEnum.APPROVED.getCode());
        updateOrderStatus(order, OrderStatusEnum.CANCELLED.getCode());

        Integer originalStatus = refundOrder.getOriginalOrderStatus();
        if (originalStatus != null && OrderStatusEnum.COMPLETED.getCode().equals(originalStatus)) {
            StockOperationUtil.rollbackStock(refundOrder.getOrderId(), orderItemMapper, productMapper);
            stockReservationService.releaseReservations(order.getId(), "退款审核通过，库存回滚");
            logger.info("退款审核通过，回滚已扣减库存: refundId={}, orderId={}",
                    refundOrder.getId(), refundOrder.getOrderId());
        } else {
            stockReservationService.releaseReservations(order.getId(), "退款审核通过，释放预占库存");
            logger.info("退款审核通过，释放预占库存（未扣减总库存）: refundId={}, orderId={}",
                    refundOrder.getId(), refundOrder.getOrderId());
        }

        logger.info("退款审核通过: refundId={}, orderId={}, userId={}",
                refundOrder.getId(), refundOrder.getOrderId(), refundOrder.getUserId());
    }

    private void handleRejectedRefund(RefundOrder refundOrder, Order order) {
        refundOrder.setStatus(RefundStatusEnum.REJECTED.getCode());

        Integer recoverStatus = refundOrder.getOriginalOrderStatus();
        if (recoverStatus != null) {
            updateOrderStatus(order, recoverStatus);
        }

        logger.info("退款审核拒绝: refundId={}, orderId={}, userId={}, reason={}",
                refundOrder.getId(), refundOrder.getOrderId(), refundOrder.getUserId(), refundOrder.getAuditRemark());
    }
}
