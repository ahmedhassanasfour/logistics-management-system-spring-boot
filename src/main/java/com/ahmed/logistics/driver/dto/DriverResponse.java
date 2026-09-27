package com.ahmed.logistics.driver.dto;

import com.ahmed.logistics.driver.entity.Driver;
import com.ahmed.logistics.driver.entity.DriverStatus;
import com.ahmed.logistics.user.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public record DriverResponse(
        Long id,
        Long userId,
        String email,
        String firstName,
        String lastName,
        String phone,
        String licenseNumber,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate licenseExpiryDate,
        DriverStatus status
) {
    public static DriverResponse fromEntity(Driver driver) {
        User user = driver.getUser();
        return new DriverResponse(
                driver.getId(),
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                driver.getPhone(),
                driver.getLicenseNumber(),
                driver.getLicenseExpiryDate(),
                driver.getStatus()
        );
    }
}
