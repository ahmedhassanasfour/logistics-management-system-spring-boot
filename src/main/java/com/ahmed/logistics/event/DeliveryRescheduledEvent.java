package com.ahmed.logistics.event;

import com.ahmed.logistics.delivery.reschedule.entity.RescheduleStatus;

import java.time.LocalDateTime;

public record DeliveryRescheduledEvent(
        Long deliveryId,
        Long shipmentId,
        Long customerId,
        LocalDateTime newScheduledDateTime,
        String reason,
        RescheduleStatus status
) {}
