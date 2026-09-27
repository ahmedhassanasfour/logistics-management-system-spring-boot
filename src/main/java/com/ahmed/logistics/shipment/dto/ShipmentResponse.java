package com.ahmed.logistics.shipment.dto;

import com.ahmed.logistics.shipment.entity.Shipment;
import com.ahmed.logistics.shipment.entity.ShipmentStatus;
import com.ahmed.logistics.shipment.entity.ShipmentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShipmentResponse(
        Long id,
        String trackingNumber,
        Long customerId,
        String customerName,
        String customerEmail,
        ShipmentStatus status,
        ShipmentType shipmentType,
        String pickupAddress,
        String pickupCity,
        String pickupPostalCode,
        String deliveryAddress,
        String deliveryCity,
        String deliveryPostalCode,
        String recipientName,
        String recipientPhone,
        String packageDescription,
        Double weightKg,
        Double lengthCm,
        Double widthCm,
        Double heightCm,
        BigDecimal basePrice,
        BigDecimal shippingFee,
        BigDecimal totalPrice,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ShipmentResponse fromEntity(Shipment shipment) {
        String customerName = null;
        String customerEmail = null;
        Long customerId = null;

        if (shipment.getCustomer() != null) {
            customerId = shipment.getCustomer().getId();
            if (shipment.getCustomer().getUser() != null) {
                customerEmail = shipment.getCustomer().getUser().getEmail();
                customerName = (shipment.getCustomer().getUser().getFirstName() + " " +
                        shipment.getCustomer().getUser().getLastName()).trim();
            }
        }

        return new ShipmentResponse(
                shipment.getId(),
                shipment.getTrackingNumber(),
                customerId,
                customerName,
                customerEmail,
                shipment.getStatus(),
                shipment.getShipmentType(),
                shipment.getPickupAddress(),
                shipment.getPickupCity(),
                shipment.getPickupPostalCode(),
                shipment.getDeliveryAddress(),
                shipment.getDeliveryCity(),
                shipment.getDeliveryPostalCode(),
                shipment.getRecipientName(),
                shipment.getRecipientPhone(),
                shipment.getPackageDescription(),
                shipment.getWeightKg(),
                shipment.getLengthCm(),
                shipment.getWidthCm(),
                shipment.getHeightCm(),
                shipment.getBasePrice(),
                shipment.getShippingFee(),
                shipment.getTotalPrice(),
                shipment.getCreatedAt(),
                shipment.getUpdatedAt()
        );
    }
}
