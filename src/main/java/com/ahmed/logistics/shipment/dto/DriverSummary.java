package com.ahmed.logistics.shipment.dto;

import com.ahmed.logistics.driver.entity.Driver;
import com.ahmed.logistics.driver.entity.DriverStatus;

public record DriverSummary(
        Long id,
        Long userId,
        String name,
        String phone,
        String licenseNumber,
        DriverStatus status
) {
    public static DriverSummary fromEntity(Driver driver) {
        if (driver == null) {
            return null;
        }
        String name = null;
        Long userId = null;
        if (driver.getUser() != null) {
            userId = driver.getUser().getId();
            name = (driver.getUser().getFirstName() + " " + driver.getUser().getLastName()).trim();
        }
        return new DriverSummary(
                driver.getId(),
                userId,
                name,
                driver.getPhone(),
                driver.getLicenseNumber(),
                driver.getStatus()
        );
    }
}
