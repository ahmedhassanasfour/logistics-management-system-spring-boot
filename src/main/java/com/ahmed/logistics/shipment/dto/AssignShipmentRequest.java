package com.ahmed.logistics.shipment.dto;

import jakarta.validation.constraints.NotNull;

public record AssignShipmentRequest(
        @NotNull(message = "Driver ID is required")
        Long driverId,

        @NotNull(message = "Vehicle ID is required")
        Long vehicleId
) {}
