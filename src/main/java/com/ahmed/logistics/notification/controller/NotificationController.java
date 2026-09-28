package com.ahmed.logistics.notification.controller;

import com.ahmed.logistics.notification.dto.NotificationResponse;
import com.ahmed.logistics.notification.dto.UnreadNotificationCountResponse;
import com.ahmed.logistics.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Notifications", description = "In-app notification history, unread counts, and read status management")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Get user notifications (paginated)", description = "Retrieves paginated notifications for the authenticated user or linked customer account.")
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @Parameter(description = "Pagination configuration")
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        Page<NotificationResponse> response = notificationService.getNotifications(authentication, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get unread notifications (paginated)", description = "Retrieves paginated unread notifications for the authenticated user.")
    @GetMapping("/unread")
    public ResponseEntity<Page<NotificationResponse>> getUnreadNotifications(
            @Parameter(description = "Pagination configuration")
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        Page<NotificationResponse> response = notificationService.getUnreadNotifications(authentication, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get unread notification count", description = "Returns the total number of unread notifications for the authenticated user.")
    @GetMapping("/unread/count")
    public ResponseEntity<UnreadNotificationCountResponse> getUnreadCount(Authentication authentication) {
        long count = notificationService.getUnreadCount(authentication);
        return ResponseEntity.ok(UnreadNotificationCountResponse.of(count));
    }

    @Operation(summary = "Mark notification as read", description = "Marks a specific notification as READ idempotently.")
    @PreAuthorize("@notificationSecurity.canAccessNotification(#notificationId, authentication)")
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @Parameter(description = "Notification ID", required = true)
            @PathVariable Long notificationId,
            Authentication authentication
    ) {
        NotificationResponse response = notificationService.markAsRead(notificationId, authentication);
        return ResponseEntity.ok(response);
    }
}
