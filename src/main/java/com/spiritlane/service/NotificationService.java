package com.spiritlane.service;

import com.spiritlane.entity.Notification;
import java.util.List;

public interface NotificationService {
    void sendNotification(Long userId, String title, String message, Notification.NotificationType type);
    List<Notification> getRecentNotifications(Long userId, int limit);
    long getUnreadCount(Long userId);
    void markAllRead(Long userId);
}
