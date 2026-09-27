package com.ahmed.logistics.shipment.tracking.controller;

import com.ahmed.logistics.shipment.tracking.dto.ShipmentTrackingResponse;
import com.ahmed.logistics.shipment.tracking.service.ShipmentTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/shipments/{shipmentId}/tracking")
@RequiredArgsConstructor
public class ShipmentTrackingController {

    private final ShipmentTrackingService shipmentTrackingService;

    @PreAuthorize("@shipmentSecurity.canRead(#shipmentId, authentication)")
    @GetMapping
    public ResponseEntity<List<ShipmentTrackingResponse>> getShipmentTracking(@PathVariable Long shipmentId) {
        List<ShipmentTrackingResponse> tracking = shipmentTrackingService.getShipmentTracking(shipmentId);
        return ResponseEntity.ok(tracking);
    }
}
