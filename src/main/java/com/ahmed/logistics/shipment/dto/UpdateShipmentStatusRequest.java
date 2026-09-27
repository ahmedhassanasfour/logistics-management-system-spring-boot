package com.ahmed.logistics.shipment.dto;

import com.ahmed.logistics.shipment.entity.ShipmentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateShipmentStatusRequest(
        @NotNull(message = "Shipment status is required")
        ShipmentStatus status
) {}
