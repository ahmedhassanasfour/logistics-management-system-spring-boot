package com.ahmed.logistics.delivery.failure.controller;

import com.ahmed.logistics.delivery.failure.dto.CreateDeliveryFailureRequest;
import com.ahmed.logistics.delivery.failure.dto.DeliveryFailureResponse;
import com.ahmed.logistics.delivery.failure.service.DeliveryFailureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DeliveryFailureController {

    private final DeliveryFailureService deliveryFailureService;

    @PreAuthorize("@deliveryFailureSecurity.canCreate(#deliveryId, authentication)")
    @PostMapping("/{deliveryId}/failure")
    public ResponseEntity<DeliveryFailureResponse> failDelivery(
            @PathVariable Long deliveryId,
            @Valid @RequestBody CreateDeliveryFailureRequest request,
            Authentication authentication
    ) {
        DeliveryFailureResponse response = deliveryFailureService.failDelivery(deliveryId, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@deliveryFailureSecurity.canRead(#deliveryId, authentication)")
    @GetMapping("/{deliveryId}/failures")
    public ResponseEntity<List<DeliveryFailureResponse>> getFailuresByDelivery(
            @PathVariable Long deliveryId,
            Authentication authentication
    ) {
        List<DeliveryFailureResponse> response = deliveryFailureService.getFailuresByDelivery(deliveryId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@deliveryFailureSecurity.canReadByShipment(#shipmentId, authentication)")
    @GetMapping("/shipment/{shipmentId}/failures")
    public ResponseEntity<List<DeliveryFailureResponse>> getFailuresByShipment(
            @PathVariable Long shipmentId,
            Authentication authentication
    ) {
        List<DeliveryFailureResponse> response = deliveryFailureService.getFailuresByShipment(shipmentId);
        return ResponseEntity.ok(response);
    }
}
