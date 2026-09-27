package com.ahmed.logistics.driver.security;

import com.ahmed.logistics.driver.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("driverSecurity")
@RequiredArgsConstructor
public class DriverSecurity {

    private final DriverRepository driverRepository;

    public boolean isOwnerOrAdmin(Long driverId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return true;
        }

        return driverRepository.findById(driverId)
                .map(driver ->
                        driver.getUser().getEmail()
                                .equalsIgnoreCase(authentication.getName()))
                .orElse(false);
    }

    public boolean canRead(Long driverId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        boolean canReadRole = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_DISPATCHER"));

        if (canReadRole) {
            return true;
        }

        return driverRepository.findById(driverId)
                .map(driver ->
                        driver.getUser().getEmail()
                                .equalsIgnoreCase(authentication.getName()))
                .orElse(false);
    }
}
