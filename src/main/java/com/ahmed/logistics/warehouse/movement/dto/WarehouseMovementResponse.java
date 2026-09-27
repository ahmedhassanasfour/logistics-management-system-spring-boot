package com.ahmed.logistics.warehouse.movement.dto;

import com.ahmed.logistics.warehouse.dto.WarehouseSummary;
import com.ahmed.logistics.warehouse.movement.entity.WarehouseMovement;

import java.time.LocalDateTime;

public record WarehouseMovementResponse(
        Long id,
        Long shipmentId,
        WarehouseSummary fromWarehouse,
        WarehouseSummary toWarehouse,
        String notes,
        LocalDateTime movedAt
) {
    public static WarehouseMovementResponse fromEntity(WarehouseMovement movement) {
        if (movement == null) {
            return null;
        }
        return new WarehouseMovementResponse(
                movement.getId(),
                movement.getShipment() != null ? movement.getShipment().getId() : null,
                WarehouseSummary.fromEntity(movement.getFromWarehouse()),
                WarehouseSummary.fromEntity(movement.getToWarehouse()),
                movement.getNotes(),
                movement.getMovedAt()
        );
    }
}
