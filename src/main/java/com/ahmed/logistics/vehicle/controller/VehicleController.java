package com.ahmed.logistics.vehicle.controller;

import com.ahmed.logistics.vehicle.dto.CreateVehicleRequest;
import com.ahmed.logistics.vehicle.dto.UpdateVehicleRequest;
import com.ahmed.logistics.vehicle.dto.VehicleResponse;
import com.ahmed.logistics.vehicle.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<VehicleResponse> createVehicle(
            @Valid @RequestBody CreateVehicleRequest request
    ) {
        VehicleResponse response = vehicleService.createVehicle(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER', 'CUSTOMER')")
    @GetMapping
    public ResponseEntity<java.util.List<VehicleResponse>> getVehicles() {
        java.util.List<VehicleResponse> responses = vehicleService.getVehicles();
        return ResponseEntity.ok(responses);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER', 'CUSTOMER')")
    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponse> getVehicleById(@PathVariable Long id) {
        VehicleResponse response = vehicleService.getVehicleById(id);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER', 'CUSTOMER')")
    @GetMapping("/plate/{plateNumber}")
    public ResponseEntity<VehicleResponse> getVehicleByPlateNumber(@PathVariable String plateNumber) {
        VehicleResponse response = vehicleService.getVehicleByPlateNumber(plateNumber);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<VehicleResponse> updateVehicle(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVehicleRequest request
    ) {
        VehicleResponse updated = vehicleService.updateVehicle(id, request);
        return ResponseEntity.ok(updated);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<VehicleResponse> updateVehicleStatus(
            @PathVariable Long id,
            @RequestParam com.ahmed.logistics.vehicle.entity.VehicleStatus status
    ) {
        VehicleResponse updated = vehicleService.updateStatus(id, status);
        return ResponseEntity.ok(updated);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }
}
