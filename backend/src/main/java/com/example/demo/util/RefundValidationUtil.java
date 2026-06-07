package com.example.demo.util;

import com.example.demo.entity.RefundOrder;
import com.example.demo.enums.RefundStatusEnum;
import com.example.demo.mapper.RefundOrderMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public final class RefundValidationUtil {

    private RefundValidationUtil() {
    }

    public static RefundOrder getRefundOrThrow(RefundOrderMapper mapper, Long id) {
        RefundOrder refund = mapper.selectById(id);
        if (refund == null) {
            throw new IllegalArgumentException("退款单不存在: " + id);
        }
        return refund;
    }

    public static RefundOrder getRefundAndCheckPermission(RefundOrderMapper mapper, Long id) {
        RefundOrder refund = getRefundOrThrow(mapper, id);
        SecurityUtil.checkResourceOwnerOrThrow(refund.getUserId());
        return refund;
    }

    public static void checkPendingStatus(RefundOrder refund) {
        if (!RefundStatusEnum.PENDING.getCode().equals(refund.getStatus())) {
            throw new IllegalArgumentException("退款单状态不允许此操作");
        }
    }

    public static void validateRefundAmount(BigDecimal refundAmount, BigDecimal orderAmount) {
        if (refundAmount.compareTo(orderAmount) > 0) {
            throw new IllegalArgumentException("退款金额不能超过订单金额");
        }
    }

    public static void checkNoPendingRefund(RefundOrderMapper mapper, Long orderId) {
        RefundOrder existingPending = mapper.selectPendingByOrderId(orderId);
        if (existingPending != null) {
            throw new IllegalArgumentException("该订单已有待审核的退款申请");
        }
    }

    public static String generateRefundNo() {
        return "REF" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
