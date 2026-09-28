package com.ahmed.logistics.notification.controller;

import com.ahmed.logistics.notification.dto.NotificationResponse;
import com.ahmed.logistics.notification.dto.UnreadNotificationCountResponse;
import com.ahmed.logistics.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        Page<NotificationResponse> response = notificationService.getNotifications(authentication, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/unread")
    public ResponseEntity<Page<NotificationResponse>> getUnreadNotifications(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        Page<NotificationResponse> response = notificationService.getUnreadNotifications(authentication, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/unread/count")
    public ResponseEntity<UnreadNotificationCountResponse> getUnreadCount(Authentication authentication) {
        long count = notificationService.getUnreadCount(authentication);
        return ResponseEntity.ok(UnreadNotificationCountResponse.of(count));
    }

    @PreAuthorize("@notificationSecurity.canAccessNotification(#notificationId, authentication)")
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable Long notificationId,
            Authentication authentication
    ) {
        NotificationResponse response = notificationService.markAsRead(notificationId, authentication);
        return ResponseEntity.ok(response);
    }
}
