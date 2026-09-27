package com.ahmed.logistics.warehouse.movement.controller;

import com.ahmed.logistics.warehouse.movement.dto.MoveShipmentToWarehouseRequest;
import com.ahmed.logistics.warehouse.movement.dto.WarehouseMovementResponse;
import com.ahmed.logistics.warehouse.movement.service.WarehouseMovementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shipments/{shipmentId}")
@RequiredArgsConstructor
public class WarehouseMovementController {

    private final WarehouseMovementService warehouseMovementService;

    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @PatchMapping("/warehouse")
    public ResponseEntity<WarehouseMovementResponse> moveShipmentToWarehouse(
            @PathVariable Long shipmentId,
            @Valid @RequestBody MoveShipmentToWarehouseRequest request
    ) {
        WarehouseMovementResponse response = warehouseMovementService.moveShipmentToWarehouse(
                shipmentId,
                request.warehouseId(),
                request.notes()
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@shipmentSecurity.canRead(#shipmentId, authentication)")
    @GetMapping("/warehouse-movements")
    public ResponseEntity<List<WarehouseMovementResponse>> getWarehouseMovements(
            @PathVariable Long shipmentId
    ) {
        List<WarehouseMovementResponse> movements = warehouseMovementService.getWarehouseMovements(shipmentId);
        return ResponseEntity.ok(movements);
    }
}
