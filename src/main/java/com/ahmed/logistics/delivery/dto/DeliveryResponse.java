package com.ahmed.logistics.delivery.dto;

import com.ahmed.logistics.delivery.entity.Delivery;
import com.ahmed.logistics.delivery.entity.DeliveryStatus;
import com.ahmed.logistics.shipment.dto.DriverSummary;

import java.time.LocalDateTime;

public record DeliveryResponse(
        Long id,
        Long shipmentId,
        String trackingNumber,
        DriverSummary driver,
        DeliveryStatus status,
        LocalDateTime startedAt,
        LocalDateTime deliveredAt,
        String deliveryNotes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DeliveryResponse fromEntity(Delivery delivery) {
        if (delivery == null) {
            return null;
        }

        Long shipmentId = null;
        String trackingNumber = null;
        if (delivery.getShipment() != null) {
            shipmentId = delivery.getShipment().getId();
            trackingNumber = delivery.getShipment().getTrackingNumber();
        }

        return new DeliveryResponse(
                delivery.getId(),
                shipmentId,
                trackingNumber,
                DriverSummary.fromEntity(delivery.getDriver()),
                delivery.getStatus(),
                delivery.getStartedAt(),
                delivery.getDeliveredAt(),
                delivery.getDeliveryNotes(),
                delivery.getCreatedAt(),
                delivery.getUpdatedAt()
        );
    }
}
