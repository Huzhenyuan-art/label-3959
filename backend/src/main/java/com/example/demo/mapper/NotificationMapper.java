package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.entity.Notification;
import org.apache.ibatis.annotations.Param;

public interface NotificationMapper extends BaseMapper<Notification> {

    IPage<Notification> selectUserNotificationPage(Page<Notification> page,
                                                   @Param("userId") Long userId,
                                                   @Param("isRead") Boolean isRead,
                                                   @Param("type") Integer type);

    int countUnread(@Param("userId") Long userId);

    int markAllAsRead(@Param("userId") Long userId);

    int markAsRead(@Param("id") Long id, @Param("userId") Long userId);
}
