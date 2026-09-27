package com.ahmed.logistics.warehouse.dto;

import com.ahmed.logistics.warehouse.entity.Warehouse;
import com.ahmed.logistics.warehouse.entity.WarehouseStatus;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class WarehouseResponseTest {

    @Test
    void passwordIsNeverPresentInWarehouseResponse() {
        // 1. Verify no field named 'password' exists in WarehouseResponse
        boolean hasPasswordField = Arrays.stream(WarehouseResponse.class.getDeclaredFields())
                .anyMatch(f -> f.getName().equalsIgnoreCase("password"));
        assertFalse(hasPasswordField, "WarehouseResponse must not declare a password field");

        // 2. Verify no getter or method exposes password
        boolean hasPasswordMethod = Arrays.stream(WarehouseResponse.class.getDeclaredMethods())
                .anyMatch(m -> m.getName().toLowerCase().contains("password"));
        assertFalse(hasPasswordMethod, "WarehouseResponse must not declare any method exposing password");

        // 3. Verify mapping from entity exposes all expected fields
        Warehouse warehouse = Warehouse.builder()
                .id(201L)
                .name("Main Distribution Hub")
                .address("100 Logistics Way")
                .city("Chicago")
                .postalCode("60601")
                .phone("+13125550199")
                .email("hub-chicago@logistics.com")
                .status(WarehouseStatus.ACTIVE)
                .build();

        WarehouseResponse response = WarehouseResponse.fromEntity(warehouse);

        assertEquals(201L, response.id());
        assertEquals("Main Distribution Hub", response.name());
        assertEquals("100 Logistics Way", response.address());
        assertEquals("Chicago", response.city());
        assertEquals("60601", response.postalCode());
        assertEquals("+13125550199", response.phone());
        assertEquals("hub-chicago@logistics.com", response.email());
        assertEquals(WarehouseStatus.ACTIVE, response.status());
    }
}
