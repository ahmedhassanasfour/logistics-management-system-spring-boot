package com.ahmed.logistics.delivery.failure.dto;

import com.ahmed.logistics.delivery.failure.entity.DeliveryFailure;
import com.ahmed.logistics.delivery.failure.entity.DeliveryFailureReason;

import java.time.LocalDateTime;

public record DeliveryFailureResponse(
        Long id,
        Long deliveryId,
        Long shipmentId,
        String trackingNumber,
        DeliveryFailureReason reason,
        String notes,
        LocalDateTime failedAt,
        LocalDateTime createdAt
) {
    public static DeliveryFailureResponse fromEntity(DeliveryFailure failure) {
        if (failure == null) {
            return null;
        }

        Long deliveryId = null;
        Long shipmentId = null;
        String trackingNumber = null;

        if (failure.getDelivery() != null) {
            deliveryId = failure.getDelivery().getId();
            if (failure.getDelivery().getShipment() != null) {
                shipmentId = failure.getDelivery().getShipment().getId();
                trackingNumber = failure.getDelivery().getShipment().getTrackingNumber();
            }
        }

        return new DeliveryFailureResponse(
                failure.getId(),
                deliveryId,
                shipmentId,
                trackingNumber,
                failure.getReason(),
                failure.getNotes(),
                failure.getFailedAt(),
                failure.getCreatedAt()
        );
    }
}
