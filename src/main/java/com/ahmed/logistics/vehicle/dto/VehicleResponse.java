package com.ahmed.logistics.vehicle.dto;

import com.ahmed.logistics.vehicle.entity.Vehicle;
import com.ahmed.logistics.vehicle.entity.VehicleStatus;
import com.ahmed.logistics.vehicle.entity.VehicleType;

public record VehicleResponse(
        Long id,
        String plateNumber,
        VehicleType type,
        VehicleStatus status,
        String brand,
        String model,
        Integer manufacturingYear,
        Double maxWeightKg
) {
    public static VehicleResponse fromEntity(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getPlateNumber(),
                vehicle.getType(),
                vehicle.getStatus(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getManufacturingYear(),
                vehicle.getMaxWeightKg()
        );
    }
}
