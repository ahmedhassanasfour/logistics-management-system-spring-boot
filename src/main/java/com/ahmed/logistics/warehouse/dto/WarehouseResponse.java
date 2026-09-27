package com.ahmed.logistics.warehouse.dto;

import com.ahmed.logistics.warehouse.entity.Warehouse;
import com.ahmed.logistics.warehouse.entity.WarehouseStatus;

public record WarehouseResponse(
        Long id,
        String name,
        String address,
        String city,
        String postalCode,
        String phone,
        String email,
        WarehouseStatus status
) {
    public static WarehouseResponse fromEntity(Warehouse warehouse) {
        return new WarehouseResponse(
                warehouse.getId(),
                warehouse.getName(),
                warehouse.getAddress(),
                warehouse.getCity(),
                warehouse.getPostalCode(),
                warehouse.getPhone(),
                warehouse.getEmail(),
                warehouse.getStatus()
        );
    }
}
