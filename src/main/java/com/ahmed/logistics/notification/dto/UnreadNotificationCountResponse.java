package com.ahmed.logistics.notification.dto;

public record UnreadNotificationCountResponse(
        long unreadCount,
        long count
) {
    public static UnreadNotificationCountResponse of(long count) {
        return new UnreadNotificationCountResponse(count, count);
    }
}
