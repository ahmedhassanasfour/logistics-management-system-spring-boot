package com.ahmed.logistics.warehouse.movement.dto;

import jakarta.validation.constraints.NotNull;

public record MoveShipmentToWarehouseRequest(
        @NotNull(message = "Warehouse ID is required")
        Long warehouseId,

        String notes
) {}
