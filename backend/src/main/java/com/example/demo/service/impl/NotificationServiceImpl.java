package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.dto.SendNotificationDTO;
import com.example.demo.entity.Notification;
import com.example.demo.enums.NotificationTypeEnum;
import com.example.demo.mapper.NotificationMapper;
import com.example.demo.service.NotificationService;
import com.example.demo.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    private final NotificationMapper notificationMapper;

    @Override
    public IPage<Notification> pageUserNotifications(int current, int size, Boolean isRead, Integer type) {
        Long userId = SecurityUtil.getCurrentUserId();
        Page<Notification> page = new Page<>(current, size);
        return notificationMapper.selectUserNotificationPage(page, userId, isRead, type);
    }

    @Override
    public int getUnreadCount() {
        Long userId = SecurityUtil.getCurrentUserId();
        return notificationMapper.countUnread(userId);
    }

    @Override
    public void markAsRead(Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        int updated = notificationMapper.markAsRead(id, userId);
        if (updated > 0) {
            log.info("标记消息已读: id={}, userId={}", id, userId);
        }
    }

    @Override
    public void markAllAsRead() {
        Long userId = SecurityUtil.getCurrentUserId();
        int updated = notificationMapper.markAllAsRead(userId);
        log.info("标记全部消息已读: userId={}, count={}", userId, updated);
    }

    @Override
    public void sendNotification(SendNotificationDTO dto) {
        Notification notification = new Notification();
        notification.setUserId(dto.getUserId());
        notification.setType(dto.getType());
        notification.setTitle(dto.getTitle());
        notification.setContent(dto.getContent());
        notification.setBizId(dto.getBizId());
        notification.setBizType(dto.getBizType());
        notification.setRead(false);
        save(notification);
        log.info("发送消息: userId={}, type={}, title={}", dto.getUserId(), dto.getType(), dto.getTitle());
    }

    @Async
    @Override
    public void sendOrderStatusNotification(Long userId, Long orderId, Integer oldStatus, Integer newStatus) {
        String statusLabel = getOrderStatusLabel(newStatus);
        String title = "订单状态变更";
        String content = String.format("您的订单 #%d 状态已变更为：%s", orderId, statusLabel);

        SendNotificationDTO dto = new SendNotificationDTO();
        dto.setUserId(userId);
        dto.setType(NotificationTypeEnum.ORDER_STATUS_CHANGE.getCode());
        dto.setTitle(title);
        dto.setContent(content);
        dto.setBizId(orderId);
        dto.setBizType("ORDER");
        sendNotification(dto);
    }

    @Async
    @Override
    public void sendRefundResultNotification(Long userId, Long orderId, boolean success, String reason) {
        String title = success ? "退款成功" : "退款失败";
        String content = success
                ? String.format("您的订单 #%d 退款已成功处理，款项将原路返回。", orderId)
                : String.format("您的订单 #%d 退款申请未通过，原因：%s", orderId, reason);

        SendNotificationDTO dto = new SendNotificationDTO();
        dto.setUserId(userId);
        dto.setType(NotificationTypeEnum.REFUND_RESULT.getCode());
        dto.setTitle(title);
        dto.setContent(content);
        dto.setBizId(orderId);
        dto.setBizType("REFUND");
        sendNotification(dto);
    }

    @Async
    @Override
    public void sendStockWarningNotification(Long productId, String productName, Integer stock) {
        String title = "库存预警";
        String content = String.format("商品「%s」当前库存仅剩余 %d，请及时补货。", productName, stock);

        SendNotificationDTO dto = new SendNotificationDTO();
        dto.setUserId(1L);
        dto.setType(NotificationTypeEnum.STOCK_WARNING.getCode());
        dto.setTitle(title);
        dto.setContent(content);
        dto.setBizId(productId);
        dto.setBizType("PRODUCT");
        sendNotification(dto);
    }

    private String getOrderStatusLabel(Integer status) {
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "已支付";
            case 2 -> "已发货";
            case 3 -> "已完成";
            case 4 -> "已取消";
            default -> "未知状态";
        };
    }
}
