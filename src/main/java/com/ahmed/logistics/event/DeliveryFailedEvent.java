package com.ahmed.logistics.event;

import com.ahmed.logistics.delivery.failure.entity.DeliveryFailureReason;

public record DeliveryFailedEvent(
        Long deliveryId,
        Long shipmentId,
        Long driverId,
        DeliveryFailureReason failureReason
) {}
