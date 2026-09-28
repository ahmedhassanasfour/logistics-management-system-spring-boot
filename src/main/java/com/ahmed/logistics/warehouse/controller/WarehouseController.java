package com.ahmed.logistics.warehouse.controller;

import com.ahmed.logistics.warehouse.dto.CreateWarehouseRequest;
import com.ahmed.logistics.warehouse.dto.UpdateWarehouseRequest;
import com.ahmed.logistics.warehouse.dto.WarehouseResponse;
import com.ahmed.logistics.warehouse.service.WarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<WarehouseResponse> createWarehouse(
            @Valid @RequestBody CreateWarehouseRequest request
    ) {
        WarehouseResponse response = warehouseService.createWarehouse(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER', 'CUSTOMER')")
    @GetMapping
    public ResponseEntity<java.util.List<WarehouseResponse>> getWarehouses() {
        java.util.List<WarehouseResponse> responses = warehouseService.getWarehouses();
        return ResponseEntity.ok(responses);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER', 'CUSTOMER')")
    @GetMapping("/{id}")
    public ResponseEntity<WarehouseResponse> getWarehouseById(@PathVariable Long id) {
        WarehouseResponse response = warehouseService.getWarehouseById(id);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER', 'CUSTOMER')")
    @GetMapping("/name/{name}")
    public ResponseEntity<WarehouseResponse> getWarehouseByName(@PathVariable String name) {
        WarehouseResponse response = warehouseService.getWarehouseByName(name);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<WarehouseResponse> updateWarehouse(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWarehouseRequest request
    ) {
        WarehouseResponse updated = warehouseService.updateWarehouse(id, request);
        return ResponseEntity.ok(updated);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWarehouse(@PathVariable Long id) {
        warehouseService.deleteWarehouse(id);
        return ResponseEntity.noContent().build();
    }
}
