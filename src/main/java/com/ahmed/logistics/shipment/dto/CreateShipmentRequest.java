package com.ahmed.logistics.shipment.dto;

import com.ahmed.logistics.shipment.entity.ShipmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateShipmentRequest(
        @NotNull(message = "Customer ID is required")
        Long customerId,

        @NotNull(message = "Shipment type is required")
        ShipmentType shipmentType,

        @NotBlank(message = "Pickup address is required")
        @Size(max = 255, message = "Pickup address must not exceed 255 characters")
        String pickupAddress,

        @NotBlank(message = "Pickup city is required")
        @Size(max = 100, message = "Pickup city must not exceed 100 characters")
        String pickupCity,

        @NotBlank(message = "Pickup postal code is required")
        @Size(max = 20, message = "Pickup postal code must not exceed 20 characters")
        String pickupPostalCode,

        @NotBlank(message = "Delivery address is required")
        @Size(max = 255, message = "Delivery address must not exceed 255 characters")
        String deliveryAddress,

        @NotBlank(message = "Delivery city is required")
        @Size(max = 100, message = "Delivery city must not exceed 100 characters")
        String deliveryCity,

        @NotBlank(message = "Delivery postal code is required")
        @Size(max = 20, message = "Delivery postal code must not exceed 20 characters")
        String deliveryPostalCode,

        @NotBlank(message = "Recipient name is required")
        @Size(max = 100, message = "Recipient name must not exceed 100 characters")
        String recipientName,

        @NotBlank(message = "Recipient phone is required")
        @Size(max = 20, message = "Recipient phone must not exceed 20 characters")
        String recipientPhone,

        @Size(max = 500, message = "Package description must not exceed 500 characters")
        String packageDescription,

        @NotNull(message = "Weight is required")
        @Positive(message = "Weight must be positive")
        Double weightKg,

        @Positive(message = "Length must be positive")
        Double lengthCm,

        @Positive(message = "Width must be positive")
        Double widthCm,

        @Positive(message = "Height must be positive")
        Double heightCm
) {}
