package com.ahmed.logistics.delivery.controller;

import com.ahmed.logistics.delivery.dto.CompleteDeliveryRequest;
import com.ahmed.logistics.delivery.dto.DeliveryResponse;
import com.ahmed.logistics.delivery.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER')")
    @PostMapping("/{shipmentId}/start")
    public ResponseEntity<DeliveryResponse> startDelivery(
            @PathVariable Long shipmentId
    ) {
        DeliveryResponse response = deliveryService.startDelivery(shipmentId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER')")
    @PostMapping("/{shipmentId}/complete")
    public ResponseEntity<DeliveryResponse> completeDelivery(
            @PathVariable Long shipmentId,
            @RequestBody(required = false) CompleteDeliveryRequest request
    ) {
        String deliveryNotes = request != null ? request.deliveryNotes() : null;
        DeliveryResponse response = deliveryService.completeDelivery(shipmentId, deliveryNotes);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@deliverySecurity.canRead(#deliveryId, authentication)")
    @GetMapping("/{deliveryId}")
    public ResponseEntity<DeliveryResponse> getDelivery(
            @PathVariable Long deliveryId,
            Authentication authentication
    ) {
        DeliveryResponse response = deliveryService.getDelivery(deliveryId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@deliverySecurity.canReadByShipment(#shipmentId, authentication)")
    @GetMapping("/shipment/{shipmentId}")
    public ResponseEntity<DeliveryResponse> getDeliveryByShipment(
            @PathVariable Long shipmentId,
            Authentication authentication
    ) {
        DeliveryResponse response = deliveryService.getDeliveryByShipment(shipmentId);
        return ResponseEntity.ok(response);
    }
}
