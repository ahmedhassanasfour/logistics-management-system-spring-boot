package com.ahmed.logistics.warehouse.dto;

import com.ahmed.logistics.warehouse.entity.Warehouse;
import com.ahmed.logistics.warehouse.entity.WarehouseStatus;

public record WarehouseSummary(
        Long id,
        String name,
        String city,
        WarehouseStatus status
) {
    public static WarehouseSummary fromEntity(Warehouse warehouse) {
        if (warehouse == null) {
            return null;
        }
        return new WarehouseSummary(
                warehouse.getId(),
                warehouse.getName(),
                warehouse.getCity(),
                warehouse.getStatus()
        );
    }
}
