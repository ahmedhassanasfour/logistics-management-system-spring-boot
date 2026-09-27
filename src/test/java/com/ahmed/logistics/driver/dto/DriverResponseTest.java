package com.ahmed.logistics.driver.dto;

import com.ahmed.logistics.driver.entity.Driver;
import com.ahmed.logistics.driver.entity.DriverStatus;
import com.ahmed.logistics.user.entity.Role;
import com.ahmed.logistics.user.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class DriverResponseTest {

    @Test
    void passwordIsNeverPresentInDriverResponse() {
        // 1. Verify no field named 'password' exists in DriverResponse
        boolean hasPasswordField = Arrays.stream(DriverResponse.class.getDeclaredFields())
                .anyMatch(f -> f.getName().equalsIgnoreCase("password"));
        assertFalse(hasPasswordField, "DriverResponse must not declare a password field");

        // 2. Verify no getter or method exposes password
        boolean hasPasswordMethod = Arrays.stream(DriverResponse.class.getDeclaredMethods())
                .anyMatch(m -> m.getName().toLowerCase().contains("password"));
        assertFalse(hasPasswordMethod, "DriverResponse must not declare any method exposing password");

        // 3. Verify mapping from entity does not expose password
        User user = User.builder()
                .id(5L)
                .email("test_driver@logistics.com")
                .password("$2a$10$superSecretHash")
                .firstName("Bob")
                .lastName("Driver")
                .role(Role.DRIVER)
                .enabled(true)
                .build();

        Driver driver = Driver.builder()
                .id(15L)
                .user(user)
                .phone("+15559876543")
                .licenseNumber("DL-123456789")
                .licenseExpiryDate(LocalDate.of(2030, 12, 31))
                .status(DriverStatus.OFFLINE)
                .build();

        DriverResponse response = DriverResponse.fromEntity(driver);

        assertEquals(15L, response.id());
        assertEquals(5L, response.userId());
        assertEquals("test_driver@logistics.com", response.email());
        assertEquals("Bob", response.firstName());
        assertEquals("Driver", response.lastName());
        assertEquals("+15559876543", response.phone());
        assertEquals("DL-123456789", response.licenseNumber());
        assertEquals(LocalDate.of(2030, 12, 31), response.licenseExpiryDate());
        assertEquals(DriverStatus.OFFLINE, response.status());
    }
}
