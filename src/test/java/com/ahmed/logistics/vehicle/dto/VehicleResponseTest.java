package com.ahmed.logistics.vehicle.dto;

import com.ahmed.logistics.vehicle.entity.Vehicle;
import com.ahmed.logistics.vehicle.entity.VehicleStatus;
import com.ahmed.logistics.vehicle.entity.VehicleType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class VehicleResponseTest {

    @Test
    void passwordIsNeverPresentInVehicleResponse() {
        // 1. Verify no field named 'password' exists in VehicleResponse
        boolean hasPasswordField = Arrays.stream(VehicleResponse.class.getDeclaredFields())
                .anyMatch(f -> f.getName().equalsIgnoreCase("password"));
        assertFalse(hasPasswordField, "VehicleResponse must not declare a password field");

        // 2. Verify no getter or method exposes password
        boolean hasPasswordMethod = Arrays.stream(VehicleResponse.class.getDeclaredMethods())
                .anyMatch(m -> m.getName().toLowerCase().contains("password"));
        assertFalse(hasPasswordMethod, "VehicleResponse must not declare any method exposing password");

        // 3. Verify mapping from entity exposes all expected fields
        Vehicle vehicle = Vehicle.builder()
                .id(101L)
                .plateNumber("XYZ-9988")
                .type(VehicleType.TRUCK)
                .status(VehicleStatus.AVAILABLE)
                .brand("Volvo")
                .model("FH16")
                .manufacturingYear(2023)
                .maxWeightKg(25000.0)
                .build();

        VehicleResponse response = VehicleResponse.fromEntity(vehicle);

        assertEquals(101L, response.id());
        assertEquals("XYZ-9988", response.plateNumber());
        assertEquals(VehicleType.TRUCK, response.type());
        assertEquals(VehicleStatus.AVAILABLE, response.status());
        assertEquals("Volvo", response.brand());
        assertEquals("FH16", response.model());
        assertEquals(2023, response.manufacturingYear());
        assertEquals(25000.0, response.maxWeightKg());
    }
}
