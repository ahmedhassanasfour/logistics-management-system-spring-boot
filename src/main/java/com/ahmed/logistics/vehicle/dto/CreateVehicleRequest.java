package com.ahmed.logistics.vehicle.dto;

import com.ahmed.logistics.vehicle.entity.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateVehicleRequest(
        @NotBlank(message = "Plate number is required")
        @Size(max = 20, message = "Plate number must not exceed 20 characters")
        String plateNumber,

        @NotNull(message = "Vehicle type is required")
        VehicleType type,

        @NotBlank(message = "Brand is required")
        @Size(max = 50, message = "Brand must not exceed 50 characters")
        String brand,

        @NotBlank(message = "Model is required")
        @Size(max = 50, message = "Model must not exceed 50 characters")
        String model,

        @NotNull(message = "Manufacturing year is required")
        Integer manufacturingYear,

        @NotNull(message = "Max weight is required")
        @Positive(message = "Max weight must be positive")
        Double maxWeightKg
) {}
