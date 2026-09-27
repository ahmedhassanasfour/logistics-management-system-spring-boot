package com.ahmed.logistics.vehicle.service;

import com.ahmed.logistics.exception.BadRequestException;
import com.ahmed.logistics.exception.ResourceNotFoundException;
import com.ahmed.logistics.vehicle.dto.CreateVehicleRequest;
import com.ahmed.logistics.vehicle.dto.UpdateVehicleRequest;
import com.ahmed.logistics.vehicle.dto.VehicleResponse;
import com.ahmed.logistics.vehicle.entity.Vehicle;
import com.ahmed.logistics.vehicle.entity.VehicleStatus;
import com.ahmed.logistics.vehicle.entity.VehicleType;
import com.ahmed.logistics.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private VehicleService vehicleService;

    private Vehicle sampleVehicle;

    @BeforeEach
    void setUp() {
        sampleVehicle = Vehicle.builder()
                .id(1L)
                .plateNumber("ABC-1234")
                .type(VehicleType.VAN)
                .status(VehicleStatus.AVAILABLE)
                .brand("Mercedes")
                .model("Sprinter")
                .manufacturingYear(2023)
                .maxWeightKg(3500.0)
                .build();
    }

    @Test
    void createVehicle_validCreationSucceedsWithInitialStatusAvailable() {
        CreateVehicleRequest request = new CreateVehicleRequest(
                "ABC-1234",
                VehicleType.VAN,
                "Mercedes",
                "Sprinter",
                2023,
                3500.0
        );

        when(vehicleRepository.existsByPlateNumber("ABC-1234")).thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(sampleVehicle);

        VehicleResponse response = vehicleService.createVehicle(request);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("ABC-1234", response.plateNumber());
        assertEquals(VehicleType.VAN, response.type());
        assertEquals(VehicleStatus.AVAILABLE, response.status());
        assertEquals("Mercedes", response.brand());
        assertEquals("Sprinter", response.model());
        assertEquals(2023, response.manufacturingYear());
        assertEquals(3500.0, response.maxWeightKg());

        verify(vehicleRepository, times(1)).save(any(Vehicle.class));
    }

    @Test
    void createVehicle_duplicatePlateNumberRejected() {
        CreateVehicleRequest request = new CreateVehicleRequest(
                "ABC-1234",
                VehicleType.VAN,
                "Mercedes",
                "Sprinter",
                2023,
                3500.0
        );

        when(vehicleRepository.existsByPlateNumber("ABC-1234")).thenReturn(true);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> vehicleService.createVehicle(request)
        );

        assertTrue(ex.getMessage().contains("already exists"));
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void getVehicleById_returnsVehicleWhenFound() {
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(sampleVehicle));

        VehicleResponse response = vehicleService.getVehicleById(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("ABC-1234", response.plateNumber());
        verify(vehicleRepository).findById(1L);
    }

    @Test
    void getVehicleById_throwsResourceNotFoundExceptionWhenMissing() {
        when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> vehicleService.getVehicleById(99L)
        );
        verify(vehicleRepository).findById(99L);
    }

    @Test
    void getVehicleByPlateNumber_returnsVehicleWhenFound() {
        when(vehicleRepository.findByPlateNumber("ABC-1234")).thenReturn(Optional.of(sampleVehicle));

        VehicleResponse response = vehicleService.getVehicleByPlateNumber("ABC-1234");

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("ABC-1234", response.plateNumber());
        verify(vehicleRepository).findByPlateNumber("ABC-1234");
    }

    @Test
    void getVehicleByPlateNumber_throwsResourceNotFoundExceptionWhenMissing() {
        when(vehicleRepository.findByPlateNumber("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> vehicleService.getVehicleByPlateNumber("UNKNOWN")
        );
        verify(vehicleRepository).findByPlateNumber("UNKNOWN");
    }

    @Test
    void updateVehicle_updatesProfileFieldsWithoutChangingStatus() {
        sampleVehicle.setStatus(VehicleStatus.IN_USE);

        UpdateVehicleRequest updateRequest = new UpdateVehicleRequest(
                "XYZ-5678",
                VehicleType.TRUCK,
                "Volvo",
                "FH",
                2024,
                20000.0
        );

        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(sampleVehicle));
        when(vehicleRepository.existsByPlateNumber("XYZ-5678")).thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleResponse response = vehicleService.updateVehicle(1L, updateRequest);

        assertNotNull(response);
        assertEquals("XYZ-5678", response.plateNumber());
        assertEquals(VehicleType.TRUCK, response.type());
        assertEquals("Volvo", response.brand());
        assertEquals("FH", response.model());
        assertEquals(2024, response.manufacturingYear());
        assertEquals(20000.0, response.maxWeightKg());
        // Operational status must remain untouched!
        assertEquals(VehicleStatus.IN_USE, response.status());

        verify(vehicleRepository).save(sampleVehicle);
    }

    @Test
    void updateVehicle_samePlateNumberAllowedWithoutDuplicateError() {
        UpdateVehicleRequest updateRequest = new UpdateVehicleRequest(
                "ABC-1234", // same plate
                VehicleType.VAN,
                "Mercedes",
                "Sprinter 2024",
                2024,
                4000.0
        );

        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(sampleVehicle));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleResponse response = vehicleService.updateVehicle(1L, updateRequest);

        assertNotNull(response);
        assertEquals("ABC-1234", response.plateNumber());
        assertEquals("Sprinter 2024", response.model());
        verify(vehicleRepository, never()).existsByPlateNumber(any());
        verify(vehicleRepository).save(sampleVehicle);
    }

    @Test
    void updateVehicle_duplicatePlateNumberRejected() {
        UpdateVehicleRequest updateRequest = new UpdateVehicleRequest(
                "TAKEN-PLATE",
                VehicleType.VAN,
                "Mercedes",
                "Sprinter",
                2023,
                3500.0
        );

        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(sampleVehicle));
        when(vehicleRepository.existsByPlateNumber("TAKEN-PLATE")).thenReturn(true);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> vehicleService.updateVehicle(1L, updateRequest)
        );

        assertTrue(ex.getMessage().contains("already exists"));
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void updateVehicle_throwsResourceNotFoundExceptionWhenMissing() {
        UpdateVehicleRequest updateRequest = new UpdateVehicleRequest(
                "XYZ-5678",
                VehicleType.CAR,
                "Toyota",
                "Yaris",
                2021,
                1200.0
        );

        when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> vehicleService.updateVehicle(99L, updateRequest)
        );
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void deleteVehicle_deletesVehicleEntity() {
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(sampleVehicle));

        vehicleService.deleteVehicle(1L);

        verify(vehicleRepository).delete(sampleVehicle);
    }

    @Test
    void deleteVehicle_throwsResourceNotFoundExceptionWhenMissing() {
        when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> vehicleService.deleteVehicle(99L)
        );
        verify(vehicleRepository, never()).delete(any());
    }
}
