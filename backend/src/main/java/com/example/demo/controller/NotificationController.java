package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.common.Result;
import com.example.demo.entity.Notification;
import com.example.demo.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/page")
    public Result<IPage<Notification>> page(@RequestParam(defaultValue = "1") int current,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) Boolean isRead,
                                            @RequestParam(required = false) Integer type) {
        return Result.ok(notificationService.pageUserNotifications(current, size, isRead, type));
    }

    @GetMapping("/unread-count")
    public Result<Integer> getUnreadCount() {
        return Result.ok(notificationService.getUnreadCount());
    }

    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return Result.ok();
    }

    @PutMapping("/read-all")
    public Result<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return Result.ok();
    }
}
