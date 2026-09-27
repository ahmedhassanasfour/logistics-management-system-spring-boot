package com.ahmed.logistics.vehicle.repository;

import com.ahmed.logistics.vehicle.entity.Vehicle;
import com.ahmed.logistics.vehicle.entity.VehicleStatus;
import com.ahmed.logistics.vehicle.entity.VehicleType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class VehicleRepositoryTest {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void saveVehicle_persistsAndGeneratesId() {
        Vehicle vehicle = Vehicle.builder()
                .plateNumber("TX-1001")
                .type(VehicleType.VAN)
                .status(VehicleStatus.AVAILABLE)
                .brand("Ford")
                .model("Transit")
                .manufacturingYear(2022)
                .maxWeightKg(3500.0)
                .build();

        Vehicle saved = vehicleRepository.saveAndFlush(vehicle);

        assertNotNull(saved.getId());
        assertEquals("TX-1001", saved.getPlateNumber());
        assertEquals(VehicleType.VAN, saved.getType());
        assertEquals(VehicleStatus.AVAILABLE, saved.getStatus());
        assertEquals("Ford", saved.getBrand());
        assertEquals("Transit", saved.getModel());
        assertEquals(2022, saved.getManufacturingYear());
        assertEquals(3500.0, saved.getMaxWeightKg());
    }

    @Test
    void findByPlateNumber_returnsVehicleWhenExists() {
        Vehicle vehicle = Vehicle.builder()
                .plateNumber("FIND-001")
                .type(VehicleType.CAR)
                .status(VehicleStatus.AVAILABLE)
                .brand("Toyota")
                .model("Corolla")
                .manufacturingYear(2021)
                .maxWeightKg(1500.0)
                .build();
        vehicleRepository.saveAndFlush(vehicle);

        Optional<Vehicle> found = vehicleRepository.findByPlateNumber("FIND-001");

        assertTrue(found.isPresent());
        assertEquals("FIND-001", found.get().getPlateNumber());
        assertEquals("Toyota", found.get().getBrand());
    }

    @Test
    void findByPlateNumber_returnsEmptyWhenNotFound() {
        Optional<Vehicle> found = vehicleRepository.findByPlateNumber("NON-EXISTENT");
        assertTrue(found.isEmpty());
    }

    @Test
    void existsByPlateNumber_returnsTrueWhenExistsAndFalseOtherwise() {
        assertFalse(vehicleRepository.existsByPlateNumber("EXISTS-TEST"));

        Vehicle vehicle = Vehicle.builder()
                .plateNumber("EXISTS-TEST")
                .type(VehicleType.MOTORCYCLE)
                .status(VehicleStatus.AVAILABLE)
                .brand("Honda")
                .model("CB500")
                .manufacturingYear(2020)
                .maxWeightKg(300.0)
                .build();
        vehicleRepository.saveAndFlush(vehicle);

        assertTrue(vehicleRepository.existsByPlateNumber("EXISTS-TEST"));
    }

    @Test
    void uniquePlateNumberConstraintEnforced() {
        Vehicle vehicle1 = Vehicle.builder()
                .plateNumber("DUPLICATE-PLATE")
                .type(VehicleType.VAN)
                .status(VehicleStatus.AVAILABLE)
                .brand("Mercedes")
                .model("Sprinter")
                .manufacturingYear(2022)
                .maxWeightKg(3200.0)
                .build();
        vehicleRepository.saveAndFlush(vehicle1);

        Vehicle vehicle2 = Vehicle.builder()
                .plateNumber("DUPLICATE-PLATE")
                .type(VehicleType.TRUCK)
                .status(VehicleStatus.AVAILABLE)
                .brand("MAN")
                .model("TGX")
                .manufacturingYear(2023)
                .maxWeightKg(18000.0)
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            vehicleRepository.saveAndFlush(vehicle2);
        });
    }

    @Test
    void statusPersistedAsStringInDatabase() {
        Vehicle vehicle = Vehicle.builder()
                .plateNumber("STATUS-TEST")
                .type(VehicleType.REFRIGERATED_TRUCK)
                .status(VehicleStatus.AVAILABLE)
                .brand("Scania")
                .model("R500")
                .manufacturingYear(2024)
                .maxWeightKg(20000.0)
                .build();
        Vehicle saved = vehicleRepository.saveAndFlush(vehicle);

        Object rawStatus = entityManager.createNativeQuery(
                "SELECT status FROM vehicles WHERE id = :id"
        ).setParameter("id", saved.getId()).getSingleResult();

        Object rawType = entityManager.createNativeQuery(
                "SELECT type FROM vehicles WHERE id = :id"
        ).setParameter("id", saved.getId()).getSingleResult();

        assertEquals("AVAILABLE", rawStatus.toString());
        assertEquals("REFRIGERATED_TRUCK", rawType.toString());
    }

    @Test
    void deleteVehicle_removesRecordSuccessfully() {
        Vehicle vehicle = Vehicle.builder()
                .plateNumber("DELETE-ME")
                .type(VehicleType.CAR)
                .status(VehicleStatus.AVAILABLE)
                .brand("Hyundai")
                .model("Elantra")
                .manufacturingYear(2022)
                .maxWeightKg(1600.0)
                .build();
        Vehicle saved = vehicleRepository.saveAndFlush(vehicle);

        vehicleRepository.delete(saved);
        vehicleRepository.flush();

        assertFalse(vehicleRepository.findById(saved.getId()).isPresent());
    }
}
