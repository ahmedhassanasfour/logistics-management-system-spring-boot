package com.ahmed.logistics.driver.controller;

import com.ahmed.logistics.driver.dto.CreateDriverRequest;
import com.ahmed.logistics.driver.dto.DriverResponse;
import com.ahmed.logistics.driver.dto.UpdateDriverRequest;
import com.ahmed.logistics.driver.service.DriverService;
import com.ahmed.logistics.user.entity.User;
import com.ahmed.logistics.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Drivers", description = "Driver registration, status, and fleet assignment")
@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;
    private final UserService userService;

    @PreAuthorize("hasRole('ADMIN') or hasRole('DRIVER')")
    @PostMapping
    public ResponseEntity<DriverResponse> createDriver(
            @Valid @RequestBody CreateDriverRequest request,
            Authentication authentication
    ) {
        Long targetUserId = request.userId();
        if (targetUserId == null && authentication != null) {
            User user = userService.findEntityByEmail(authentication.getName());
            targetUserId = user.getId();
        }
        DriverResponse response = driverService.createDriver(targetUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<DriverResponse> getMyProfile(Authentication authentication) {
        User user = userService.findEntityByEmail(authentication.getName());
        DriverResponse response = driverService.getDriverByUserId(user.getId());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @GetMapping
    public ResponseEntity<java.util.List<DriverResponse>> getDrivers() {
        java.util.List<DriverResponse> responses = driverService.getDrivers();
        return ResponseEntity.ok(responses);
    }

    @PreAuthorize("@driverSecurity.canRead(#id, authentication)")
    @GetMapping("/{id}")
    public ResponseEntity<DriverResponse> getDriverById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        DriverResponse driver = driverService.getDriverById(id);
        return ResponseEntity.ok(driver);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('DISPATCHER') or @driverSecurity.isOwnerOrAdmin(#id, authentication)")
    @PatchMapping("/{id}/status")
    public ResponseEntity<DriverResponse> updateDriverStatus(
            @PathVariable Long id,
            @RequestParam com.ahmed.logistics.driver.entity.DriverStatus status,
            Authentication authentication
    ) {
        DriverResponse updated = driverService.updateStatus(id, status);
        return ResponseEntity.ok(updated);
    }

    @PreAuthorize("@driverSecurity.isOwnerOrAdmin(#id, authentication)")
    @PutMapping("/{id}")
    public ResponseEntity<DriverResponse> updateDriver(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDriverRequest request,
            Authentication authentication
    ) {
        DriverResponse updated = driverService.updateDriver(id, request);
        return ResponseEntity.ok(updated);
    }

    @PreAuthorize("@driverSecurity.isOwnerOrAdmin(#id, authentication)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDriver(
            @PathVariable Long id,
            Authentication authentication
    ) {
        driverService.deleteDriver(id);
        return ResponseEntity.noContent().build();
    }
}
