package org.example.unibooker.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.unibooker.domain.notification.model.NotificationStatus;
import org.example.unibooker.domain.notification.model.NotificationType;
import org.example.unibooker.domain.notification.model.entity.Notifications;
import org.example.unibooker.domain.notification.model.dto.NotificationDto;
import org.example.unibooker.domain.notification.repository.NotificationRepository;
import org.example.unibooker.domain.user.model.UserRole;
import org.example.unibooker.domain.user.model.UserStatus;
import org.example.unibooker.domain.user.model.entity.Users;
import org.example.unibooker.domain.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.MissingFormatArgumentException;

/**
 * 알림 서비스
 * - 알림 저장 (DB)
 * - 알림 목록 조회
 * - 알림 읽음 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * 알림 저장 (WebSocket 발송 없음)
     */
    @Transactional
    public void saveNotification(NotificationType type, Users targetUser, Object... args) {
        String title;
        String message;

        try {
            if (args != null && args.length > 0) {
                title = String.format(type.getTitle(), args);
                message = String.format(type.getMessage(), args);
            } else {
                title = type.getTitle();
                message = type.getMessage();
            }
        } catch (MissingFormatArgumentException e) {
            title = type.getTitle();
            message = type.getMessage();
        }

        NotificationDto.NotificationReq req = NotificationDto.NotificationReq.builder()
                .category(type)
                .title(title)
                .message(message)
                .build();

        Notifications notification = req.toEntity(targetUser);
        notification.setStatus(NotificationStatus.SENT);
        notificationRepository.save(notification);

        log.info("✅ 알림 저장 - userId: {}, type: {}, title: {}", targetUser.getId(), type, title);
    }

    /**
     * 특정 역할의 모든 사용자에게 알림 저장
     */
    @Transactional
    public void saveNotificationToRole(NotificationType type, UserRole role, Object... args) {
        List<Users> users = userRepository.findByRoleAndStatus(role, UserStatus.ACTIVE, Pageable.unpaged()).getContent();

        for (Users user : users) {
            saveNotification(type, user, args);
        }
    }

    /**
     * 알림 목록 조회
     */
    public Page<NotificationDto.NotificationRes> getUserNotifications(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notifications> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return NotificationDto.NotificationRes.fromEntityList(notifications);
    }

    /**
     * 알림 읽음 처리
     */
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notifications notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 알림이 존재하지 않습니다. id=" + notificationId));

        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }
}