package com.example.demo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.dto.SendNotificationDTO;
import com.example.demo.entity.Notification;

public interface NotificationService extends IService<Notification> {

    IPage<Notification> pageUserNotifications(int current, int size, Boolean isRead, Integer type);

    int getUnreadCount();

    void markAsRead(Long id);

    void markAllAsRead();

    void sendNotification(SendNotificationDTO dto);

    void sendOrderStatusNotification(Long userId, Long orderId, Integer oldStatus, Integer newStatus);

    void sendRefundResultNotification(Long userId, Long orderId, boolean success, String reason);

    void sendStockWarningNotification(Long productId, String productName, Integer stock);
}
