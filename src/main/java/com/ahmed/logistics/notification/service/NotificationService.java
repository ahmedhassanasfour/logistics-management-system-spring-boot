package com.ahmed.logistics.notification.service;

import com.ahmed.logistics.customer.entity.Customer;
import com.ahmed.logistics.customer.repository.CustomerRepository;
import com.ahmed.logistics.exception.BadRequestException;
import com.ahmed.logistics.exception.ForbiddenException;
import com.ahmed.logistics.exception.ResourceNotFoundException;
import com.ahmed.logistics.exception.UnauthorizedException;
import com.ahmed.logistics.notification.dto.NotificationResponse;
import com.ahmed.logistics.notification.entity.Notification;
import com.ahmed.logistics.notification.entity.NotificationStatus;
import com.ahmed.logistics.notification.entity.NotificationType;
import com.ahmed.logistics.notification.repository.NotificationRepository;
import com.ahmed.logistics.user.entity.User;
import com.ahmed.logistics.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserService userService;
    private final CustomerRepository customerRepository;

    @Transactional
    public NotificationResponse createNotification(Long userId, NotificationType type, String title, String message) {
        log.info("Creating notification of type: {} for user/customer ID: {}", type, userId);

        if (userId == null) {
            throw new BadRequestException("User ID must not be null");
        }
        if (type == null) {
            throw new BadRequestException("Notification type must not be null");
        }
        if (title == null || title.isBlank()) {
            throw new BadRequestException("Notification title must not be blank");
        }
        if (message == null || message.isBlank()) {
            throw new BadRequestException("Notification message must not be blank");
        }

        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title.trim())
                .message(message.trim())
                .status(NotificationStatus.UNREAD)
                .createdAt(LocalDateTime.now())
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Notification created with ID: {} for user/customer ID: {}", saved.getId(), userId);
        return NotificationResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Authentication authentication, Pageable pageable) {
        List<Long> userIds = resolveUserIds(authentication);
        log.info("Fetching notifications for user IDs: {}", userIds);
        return notificationRepository.findByUserIdInOrderByCreatedAtDesc(userIds, pageable)
                .map(NotificationResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUnreadNotifications(Authentication authentication, Pageable pageable) {
        List<Long> userIds = resolveUserIds(authentication);
        log.info("Fetching unread notifications for user IDs: {}", userIds);
        return notificationRepository.findByUserIdInAndStatusOrderByCreatedAtDesc(userIds, NotificationStatus.UNREAD, pageable)
                .map(NotificationResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Authentication authentication) {
        List<Long> userIds = resolveUserIds(authentication);
        return notificationRepository.countByUserIdInAndStatus(userIds, NotificationStatus.UNREAD);
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Authentication authentication) {
        log.info("Attempting to mark notification ID: {} as READ", notificationId);

        if (notificationId == null) {
            throw new BadRequestException("Notification ID must not be null");
        }

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + notificationId));

        List<Long> userIds = resolveUserIds(authentication);
        if (!userIds.contains(notification.getUserId())) {
            log.warn("User with IDs {} forbidden from accessing notification ID: {} owned by ID: {}",
                    userIds, notificationId, notification.getUserId());
            throw new ForbiddenException("You are not authorized to access this notification");
        }

        if (notification.getStatus() == NotificationStatus.READ) {
            log.info("Notification ID: {} is already READ (idempotent operation)", notificationId);
            return NotificationResponse.fromEntity(notification);
        }

        notification.setStatus(NotificationStatus.READ);
        notification.setReadAt(LocalDateTime.now());
        Notification saved = notificationRepository.save(notification);

        log.info("Notification ID: {} marked as READ successfully", notificationId);
        return NotificationResponse.fromEntity(saved);
    }

    public List<Long> resolveUserIds(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User is not authenticated");
        }

        User user = userService.findEntityByEmail(authentication.getName());
        List<Long> ids = new ArrayList<>();
        ids.add(user.getId());

        customerRepository.findByUserId(user.getId())
                .map(Customer::getId)
                .ifPresent(ids::add);

        return ids;
    }
}
