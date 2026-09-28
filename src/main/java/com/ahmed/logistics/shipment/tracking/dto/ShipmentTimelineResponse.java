package com.ahmed.logistics.shipment.tracking.dto;

import com.ahmed.logistics.shipment.dto.ShipmentResponse;
import com.ahmed.logistics.shipment.entity.ShipmentStatus;

import java.util.List;

public record ShipmentTimelineResponse(
        Long shipmentId,
        String trackingNumber,
        ShipmentStatus currentStatus,
        ShipmentResponse shipment,
        List<ShipmentTrackingResponse> timeline
) {
}
