package com.ahmed.logistics.shipment.dto;

import com.ahmed.logistics.vehicle.entity.Vehicle;
import com.ahmed.logistics.vehicle.entity.VehicleStatus;
import com.ahmed.logistics.vehicle.entity.VehicleType;

public record VehicleSummary(
        Long id,
        String plateNumber,
        VehicleType type,
        VehicleStatus status
) {
    public static VehicleSummary fromEntity(Vehicle vehicle) {
        if (vehicle == null) {
            return null;
        }
        return new VehicleSummary(
                vehicle.getId(),
                vehicle.getPlateNumber(),
                vehicle.getType(),
                vehicle.getStatus()
        );
    }
}
