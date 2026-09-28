package com.ahmed.logistics.event;

public record ShipmentDeliveredEvent(
        Long shipmentId,
        String trackingNumber,
        Long customerId
) {}
