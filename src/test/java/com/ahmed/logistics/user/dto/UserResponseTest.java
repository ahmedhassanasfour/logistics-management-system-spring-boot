package com.ahmed.logistics.user.dto;

import com.ahmed.logistics.user.entity.Role;
import com.ahmed.logistics.user.entity.User;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class UserResponseTest {

    @Test
    void passwordIsNotExposedByUserResponse() {
        // Verify no field named 'password' exists in UserResponse
        boolean hasPasswordField = Arrays.stream(UserResponse.class.getDeclaredFields())
                .anyMatch(field -> field.getName().equalsIgnoreCase("password"));
        assertFalse(hasPasswordField, "UserResponse must not declare a password field");

        // Verify no method named 'password' or 'getPassword' exists
        boolean hasPasswordMethod = Arrays.stream(UserResponse.class.getDeclaredMethods())
                .anyMatch(method -> method.getName().toLowerCase().contains("password"));
        assertFalse(hasPasswordMethod, "UserResponse must not declare any method exposing password");

        // Verify entity mapping does not leak password
        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("secretHash123")
                .firstName("Ahmed")
                .lastName("Ali")
                .role(Role.ADMIN)
                .enabled(true)
                .createdAt(now)
                .build();

        UserResponse response = UserResponse.fromEntity(user);

        assertEquals(1L, response.id());
        assertEquals("test@example.com", response.email());
        assertEquals("Ahmed", response.firstName());
        assertEquals("Ali", response.lastName());
        assertEquals(Role.ADMIN, response.role());
        assertTrue(response.enabled());
        assertEquals(now, response.createdAt());
    }
}
