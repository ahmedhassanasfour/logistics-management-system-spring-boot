package com.ahmed.logistics.shipment.controller;

import com.ahmed.logistics.shipment.dto.CreateShipmentRequest;
import com.ahmed.logistics.shipment.dto.ShipmentResponse;
import com.ahmed.logistics.shipment.dto.UpdateShipmentRequest;
import com.ahmed.logistics.shipment.dto.UpdateShipmentStatusRequest;
import com.ahmed.logistics.shipment.service.ShipmentLifecycleService;
import com.ahmed.logistics.shipment.service.ShipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;
    private final ShipmentLifecycleService shipmentLifecycleService;

    @PreAuthorize("@shipmentSecurity.canCreate(#request.customerId(), authentication)")
    @PostMapping
    public ResponseEntity<ShipmentResponse> createShipment(
            @Valid @RequestBody CreateShipmentRequest request,
            Authentication authentication
    ) {
        ShipmentResponse response = shipmentService.createShipment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("@shipmentSecurity.canRead(#id, authentication)")
    @GetMapping("/{id}")
    public ResponseEntity<ShipmentResponse> getShipmentById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        ShipmentResponse response = shipmentService.getShipmentById(id);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@shipmentSecurity.canReadByTracking(#trackingNumber, authentication)")
    @GetMapping("/tracking/{trackingNumber}")
    public ResponseEntity<ShipmentResponse> getShipmentByTrackingNumber(
            @PathVariable String trackingNumber,
            Authentication authentication
    ) {
        ShipmentResponse response = shipmentService.getShipmentByTrackingNumber(trackingNumber);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@shipmentSecurity.canReadCustomerShipments(#customerId, authentication)")
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<Page<ShipmentResponse>> getCustomerShipments(
            @PathVariable Long customerId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        Page<ShipmentResponse> response = shipmentService.getCustomerShipments(customerId, pageable);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@shipmentSecurity.canModify(#id, authentication)")
    @PutMapping("/{id}")
    public ResponseEntity<ShipmentResponse> updateShipment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateShipmentRequest request,
            Authentication authentication
    ) {
        ShipmentResponse response = shipmentService.updateShipment(id, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ShipmentResponse> updateShipmentStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateShipmentStatusRequest request
    ) {
        ShipmentResponse response = shipmentLifecycleService.transitionStatus(id, request.status());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@shipmentSecurity.canModify(#id, authentication)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShipment(
            @PathVariable Long id,
            Authentication authentication
    ) {
        shipmentService.deleteShipment(id);
        return ResponseEntity.noContent().build();
    }
}
