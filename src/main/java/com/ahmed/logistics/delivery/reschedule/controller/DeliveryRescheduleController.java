package com.ahmed.logistics.delivery.reschedule.controller;

import com.ahmed.logistics.delivery.dto.DeliveryResponse;
import com.ahmed.logistics.delivery.reschedule.dto.CreateRescheduleRequest;
import com.ahmed.logistics.delivery.reschedule.dto.RescheduleResponse;
import com.ahmed.logistics.delivery.reschedule.service.DeliveryRescheduleService;
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
public class DeliveryRescheduleController {

    private final DeliveryRescheduleService deliveryRescheduleService;

    @PreAuthorize("@deliveryRescheduleSecurity.canCreate(#deliveryId, authentication)")
    @PostMapping("/{deliveryId}/reschedule")
    public ResponseEntity<RescheduleResponse> reschedule(
            @PathVariable Long deliveryId,
            @Valid @RequestBody CreateRescheduleRequest request
    ) {
        RescheduleResponse response = deliveryRescheduleService.reschedule(deliveryId, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@deliveryRescheduleSecurity.canRead(#rescheduleId, authentication)")
    @GetMapping("/reschedules/{rescheduleId}")
    public ResponseEntity<RescheduleResponse> getRescheduleById(
            @PathVariable Long rescheduleId,
            Authentication authentication
    ) {
        RescheduleResponse response = deliveryRescheduleService.getRescheduleById(rescheduleId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@deliveryRescheduleSecurity.canReadByShipment(#shipmentId, authentication)")
    @GetMapping("/shipment/{shipmentId}/reschedules")
    public ResponseEntity<List<RescheduleResponse>> getReschedulesByShipment(
            @PathVariable Long shipmentId,
            Authentication authentication
    ) {
        List<RescheduleResponse> response = deliveryRescheduleService.getReschedulesByShipment(shipmentId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @PostMapping("/shipment/{shipmentId}/new-attempt")
    public ResponseEntity<DeliveryResponse> createNewDeliveryAttempt(
            @PathVariable Long shipmentId
    ) {
        DeliveryResponse response = deliveryRescheduleService.createNewDeliveryAttempt(shipmentId);
        return ResponseEntity.ok(response);
    }
}
