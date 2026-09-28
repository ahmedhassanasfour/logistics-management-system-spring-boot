package com.ahmed.logistics.delivery.reschedule.dto;

import com.ahmed.logistics.delivery.reschedule.entity.DeliveryReschedule;
import com.ahmed.logistics.delivery.reschedule.entity.RescheduleStatus;

import java.time.LocalDateTime;

public record RescheduleResponse(
        Long id,
        Long shipmentId,
        String trackingNumber,
        Long failedDeliveryId,
        LocalDateTime scheduledAt,
        String reason,
        String notes,
        RescheduleStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static RescheduleResponse fromEntity(DeliveryReschedule reschedule) {
        if (reschedule == null) {
            return null;
        }

        Long shipmentId = null;
        String trackingNumber = null;
        Long failedDeliveryId = null;

        if (reschedule.getShipment() != null) {
            shipmentId = reschedule.getShipment().getId();
            trackingNumber = reschedule.getShipment().getTrackingNumber();
        }

        if (reschedule.getFailedDelivery() != null) {
            failedDeliveryId = reschedule.getFailedDelivery().getId();
        }

        return new RescheduleResponse(
                reschedule.getId(),
                shipmentId,
                trackingNumber,
                failedDeliveryId,
                reschedule.getScheduledAt(),
                reschedule.getReason(),
                reschedule.getNotes(),
                reschedule.getStatus(),
                reschedule.getCreatedAt(),
                reschedule.getUpdatedAt()
        );
    }
}
