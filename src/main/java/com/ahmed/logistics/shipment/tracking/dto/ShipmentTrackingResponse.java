package com.ahmed.logistics.shipment.tracking.dto;

import com.ahmed.logistics.shipment.entity.ShipmentStatus;
import com.ahmed.logistics.shipment.tracking.entity.ShipmentTracking;

import java.time.LocalDateTime;

public record ShipmentTrackingResponse(
        Long id,
        Long shipmentId,
        ShipmentStatus status,
        String description,
        String location,
        LocalDateTime createdAt
) {
    public static ShipmentTrackingResponse fromEntity(ShipmentTracking tracking) {
        return new ShipmentTrackingResponse(
                tracking.getId(),
                tracking.getShipment() != null ? tracking.getShipment().getId() : null,
                tracking.getStatus(),
                tracking.getDescription(),
                tracking.getLocation(),
                tracking.getCreatedAt()
        );
    }
}
