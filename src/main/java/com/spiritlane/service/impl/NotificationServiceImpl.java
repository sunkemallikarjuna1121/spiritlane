package com.spiritlane.service.impl;

import com.spiritlane.entity.Notification;
import com.spiritlane.entity.User;
import com.spiritlane.repository.NotificationRepository;
import com.spiritlane.repository.UserRepository;
import com.spiritlane.service.NotificationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void sendNotification(Long userId, String title, String message,
                                  Notification.NotificationType type) {
        userRepository.findById(userId).ifPresent(user -> {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setType(type);
            notification.setIsRead(false);
            notificationRepository.save(notification);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getRecentNotifications(Long userId, int limit) {
        PageRequest page = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, page);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    public void markAllRead(Long userId) {
        notificationRepository.markAllReadByUserId(userId);
    }
}
