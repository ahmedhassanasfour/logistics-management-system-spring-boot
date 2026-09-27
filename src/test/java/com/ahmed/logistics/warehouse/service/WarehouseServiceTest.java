package com.ahmed.logistics.warehouse.service;

import com.ahmed.logistics.exception.BadRequestException;
import com.ahmed.logistics.exception.ResourceNotFoundException;
import com.ahmed.logistics.warehouse.dto.CreateWarehouseRequest;
import com.ahmed.logistics.warehouse.dto.UpdateWarehouseRequest;
import com.ahmed.logistics.warehouse.dto.WarehouseResponse;
import com.ahmed.logistics.warehouse.entity.Warehouse;
import com.ahmed.logistics.warehouse.entity.WarehouseStatus;
import com.ahmed.logistics.warehouse.repository.WarehouseRepository;
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
class WarehouseServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @InjectMocks
    private WarehouseService warehouseService;

    private Warehouse sampleWarehouse;

    @BeforeEach
    void setUp() {
        sampleWarehouse = Warehouse.builder()
                .id(1L)
                .name("Central Depot")
                .address("100 Depot Way")
                .city("Atlanta")
                .postalCode("30301")
                .phone("+14045550111")
                .email("central@logistics.com")
                .status(WarehouseStatus.ACTIVE)
                .build();
    }

    @Test
    void createWarehouse_validCreationSucceedsWithInitialStatusActive() {
        CreateWarehouseRequest request = new CreateWarehouseRequest(
                "Central Depot",
                "100 Depot Way",
                "Atlanta",
                "30301",
                "+14045550111",
                "central@logistics.com"
        );

        when(warehouseRepository.existsByName("Central Depot")).thenReturn(false);
        when(warehouseRepository.save(any(Warehouse.class))).thenReturn(sampleWarehouse);

        WarehouseResponse response = warehouseService.createWarehouse(request);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Central Depot", response.name());
        assertEquals("100 Depot Way", response.address());
        assertEquals("Atlanta", response.city());
        assertEquals("30301", response.postalCode());
        assertEquals("+14045550111", response.phone());
        assertEquals("central@logistics.com", response.email());
        assertEquals(WarehouseStatus.ACTIVE, response.status());

        verify(warehouseRepository, times(1)).save(any(Warehouse.class));
    }

    @Test
    void createWarehouse_duplicateNameRejected() {
        CreateWarehouseRequest request = new CreateWarehouseRequest(
                "Central Depot",
                "100 Depot Way",
                "Atlanta",
                "30301",
                "+14045550111",
                "central@logistics.com"
        );

        when(warehouseRepository.existsByName("Central Depot")).thenReturn(true);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> warehouseService.createWarehouse(request)
        );

        assertTrue(ex.getMessage().contains("already exists"));
        verify(warehouseRepository, never()).save(any());
    }

    @Test
    void getWarehouseById_returnsWarehouseWhenFound() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(sampleWarehouse));

        WarehouseResponse response = warehouseService.getWarehouseById(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Central Depot", response.name());
        verify(warehouseRepository).findById(1L);
    }

    @Test
    void getWarehouseById_throwsResourceNotFoundExceptionWhenMissing() {
        when(warehouseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> warehouseService.getWarehouseById(99L)
        );
        verify(warehouseRepository).findById(99L);
    }

    @Test
    void getWarehouseByName_returnsWarehouseWhenFound() {
        when(warehouseRepository.findByName("Central Depot")).thenReturn(Optional.of(sampleWarehouse));

        WarehouseResponse response = warehouseService.getWarehouseByName("Central Depot");

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Central Depot", response.name());
        verify(warehouseRepository).findByName("Central Depot");
    }

    @Test
    void getWarehouseByName_throwsResourceNotFoundExceptionWhenMissing() {
        when(warehouseRepository.findByName("NonExistent")).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> warehouseService.getWarehouseByName("NonExistent")
        );
        verify(warehouseRepository).findByName("NonExistent");
    }

    @Test
    void updateWarehouse_updatesProfileFieldsWithoutChangingStatus() {
        sampleWarehouse.setStatus(WarehouseStatus.MAINTENANCE);

        UpdateWarehouseRequest updateRequest = new UpdateWarehouseRequest(
                "Updated Depot Name",
                "200 New Depot Rd",
                "Savannah",
                "31401",
                "+19125550222",
                "updated-depot@logistics.com"
        );

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(sampleWarehouse));
        when(warehouseRepository.existsByName("Updated Depot Name")).thenReturn(false);
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WarehouseResponse response = warehouseService.updateWarehouse(1L, updateRequest);

        assertNotNull(response);
        assertEquals("Updated Depot Name", response.name());
        assertEquals("200 New Depot Rd", response.address());
        assertEquals("Savannah", response.city());
        assertEquals("31401", response.postalCode());
        assertEquals("+19125550222", response.phone());
        assertEquals("updated-depot@logistics.com", response.email());
        // Status remains unchanged
        assertEquals(WarehouseStatus.MAINTENANCE, response.status());

        verify(warehouseRepository).save(sampleWarehouse);
    }

    @Test
    void updateWarehouse_sameNameAllowedWithoutDuplicateError() {
        UpdateWarehouseRequest updateRequest = new UpdateWarehouseRequest(
                "Central Depot", // same name
                "100 Depot Way Suite B",
                "Atlanta",
                "30301",
                "+14045550111",
                "central@logistics.com"
        );

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(sampleWarehouse));
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WarehouseResponse response = warehouseService.updateWarehouse(1L, updateRequest);

        assertNotNull(response);
        assertEquals("Central Depot", response.name());
        assertEquals("100 Depot Way Suite B", response.address());
        verify(warehouseRepository, never()).existsByName(any());
        verify(warehouseRepository).save(sampleWarehouse);
    }

    @Test
    void updateWarehouse_duplicateNameRejected() {
        UpdateWarehouseRequest updateRequest = new UpdateWarehouseRequest(
                "Another Existing Warehouse",
                "100 Depot Way",
                "Atlanta",
                "30301",
                "+14045550111",
                "central@logistics.com"
        );

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(sampleWarehouse));
        when(warehouseRepository.existsByName("Another Existing Warehouse")).thenReturn(true);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> warehouseService.updateWarehouse(1L, updateRequest)
        );

        assertTrue(ex.getMessage().contains("already exists"));
        verify(warehouseRepository, never()).save(any());
    }

    @Test
    void updateWarehouse_throwsResourceNotFoundExceptionWhenMissing() {
        UpdateWarehouseRequest updateRequest = new UpdateWarehouseRequest(
                "New Name",
                "Address",
                "City",
                "12345",
                "+1234567890",
                "test@logistics.com"
        );

        when(warehouseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> warehouseService.updateWarehouse(99L, updateRequest)
        );
        verify(warehouseRepository, never()).save(any());
    }

    @Test
    void deleteWarehouse_deletesWarehouseEntity() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(sampleWarehouse));

        warehouseService.deleteWarehouse(1L);

        verify(warehouseRepository).delete(sampleWarehouse);
    }

    @Test
    void deleteWarehouse_throwsResourceNotFoundExceptionWhenMissing() {
        when(warehouseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> warehouseService.deleteWarehouse(99L)
        );
        verify(warehouseRepository, never()).delete(any());
    }
}
