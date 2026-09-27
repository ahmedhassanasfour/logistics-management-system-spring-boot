package com.ahmed.logistics.vehicle.controller;

import com.ahmed.logistics.exception.ForbiddenException;
import com.ahmed.logistics.exception.UnauthorizedException;
import com.ahmed.logistics.vehicle.dto.CreateVehicleRequest;
import com.ahmed.logistics.vehicle.dto.UpdateVehicleRequest;
import com.ahmed.logistics.vehicle.dto.VehicleResponse;
import com.ahmed.logistics.vehicle.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    public ResponseEntity<VehicleResponse> createVehicle(
            @Valid @RequestBody CreateVehicleRequest request,
            Authentication authentication
    ) {
        validateAdmin(authentication);
        VehicleResponse response = vehicleService.createVehicle(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponse> getVehicleById(@PathVariable Long id) {
        VehicleResponse response = vehicleService.getVehicleById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/plate/{plateNumber}")
    public ResponseEntity<VehicleResponse> getVehicleByPlateNumber(@PathVariable String plateNumber) {
        VehicleResponse response = vehicleService.getVehicleByPlateNumber(plateNumber);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<VehicleResponse> updateVehicle(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVehicleRequest request,
            Authentication authentication
    ) {
        validateAdmin(authentication);
        VehicleResponse updated = vehicleService.updateVehicle(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehicle(
            @PathVariable Long id,
            Authentication authentication
    ) {
        validateAdmin(authentication);
        vehicleService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }

    private void validateAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            throw new ForbiddenException("Only administrators have permission to perform this operation");
        }
    }
}
