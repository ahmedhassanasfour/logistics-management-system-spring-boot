package com.ahmed.logistics.driver.controller;

import com.ahmed.logistics.driver.dto.CreateDriverRequest;
import com.ahmed.logistics.driver.dto.DriverResponse;
import com.ahmed.logistics.driver.dto.UpdateDriverRequest;
import com.ahmed.logistics.driver.service.DriverService;
import com.ahmed.logistics.exception.ForbiddenException;
import com.ahmed.logistics.exception.UnauthorizedException;
import com.ahmed.logistics.user.entity.User;
import com.ahmed.logistics.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;
    private final UserService userService;

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

    @GetMapping("/me")
    public ResponseEntity<DriverResponse> getMyProfile(Authentication authentication) {
        User user = userService.findEntityByEmail(authentication.getName());
        DriverResponse response = driverService.getDriverByUserId(user.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DriverResponse> getDriverById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        DriverResponse driver = driverService.getDriverById(id);
        validateOwnershipOrAdmin(driver.email(), authentication);
        return ResponseEntity.ok(driver);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DriverResponse> updateDriver(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDriverRequest request,
            Authentication authentication
    ) {
        DriverResponse existing = driverService.getDriverById(id);
        validateOwnershipOrAdmin(existing.email(), authentication);
        DriverResponse updated = driverService.updateDriver(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDriver(
            @PathVariable Long id,
            Authentication authentication
    ) {
        DriverResponse existing = driverService.getDriverById(id);
        validateOwnershipOrAdmin(existing.email(), authentication);
        driverService.deleteDriver(id);
        return ResponseEntity.noContent().build();
    }

    private void validateOwnershipOrAdmin(String driverEmail, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !authentication.getName().equalsIgnoreCase(driverEmail)) {
            throw new ForbiddenException("You do not have permission to access this driver profile");
        }
    }
}
