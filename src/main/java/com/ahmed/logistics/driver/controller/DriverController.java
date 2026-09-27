package com.ahmed.logistics.driver.controller;

import com.ahmed.logistics.driver.dto.CreateDriverRequest;
import com.ahmed.logistics.driver.dto.DriverResponse;
import com.ahmed.logistics.driver.dto.UpdateDriverRequest;
import com.ahmed.logistics.driver.service.DriverService;
import com.ahmed.logistics.user.entity.User;
import com.ahmed.logistics.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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

    @PreAuthorize("@driverSecurity.canRead(#id, authentication)")
    @GetMapping("/{id}")
    public ResponseEntity<DriverResponse> getDriverById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        DriverResponse driver = driverService.getDriverById(id);
        return ResponseEntity.ok(driver);
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
