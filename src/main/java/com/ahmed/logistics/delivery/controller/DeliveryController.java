package com.ahmed.logistics.delivery.controller;

import com.ahmed.logistics.delivery.dto.CompleteDeliveryRequest;
import com.ahmed.logistics.delivery.dto.DeliveryResponse;
import com.ahmed.logistics.delivery.service.DeliveryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Deliveries", description = "Delivery dispatch, tracking, and completion")
@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PreAuthorize("@deliverySecurity.canManageShipmentDelivery(#shipmentId, authentication)")
    @PostMapping("/{shipmentId}/start")
    public ResponseEntity<DeliveryResponse> startDelivery(
            @PathVariable Long shipmentId,
            Authentication authentication
    ) {
        DeliveryResponse response = deliveryService.startDelivery(shipmentId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@deliverySecurity.canManageShipmentDelivery(#shipmentId, authentication)")
    @PostMapping("/{shipmentId}/complete")
    public ResponseEntity<DeliveryResponse> completeDelivery(
            @PathVariable Long shipmentId,
            @RequestBody(required = false) CompleteDeliveryRequest request,
            Authentication authentication
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
