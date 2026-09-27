package com.ahmed.logistics.warehouse.movement.service;

import com.ahmed.logistics.exception.BadRequestException;
import com.ahmed.logistics.exception.ResourceNotFoundException;
import com.ahmed.logistics.shipment.entity.Shipment;
import com.ahmed.logistics.shipment.entity.ShipmentStatus;
import com.ahmed.logistics.shipment.repository.ShipmentRepository;
import com.ahmed.logistics.shipment.tracking.service.ShipmentTrackingService;
import com.ahmed.logistics.warehouse.entity.Warehouse;
import com.ahmed.logistics.warehouse.entity.WarehouseStatus;
import com.ahmed.logistics.warehouse.movement.dto.WarehouseMovementResponse;
import com.ahmed.logistics.warehouse.movement.entity.WarehouseMovement;
import com.ahmed.logistics.warehouse.movement.repository.WarehouseMovementRepository;
import com.ahmed.logistics.warehouse.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WarehouseMovementServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private WarehouseMovementRepository warehouseMovementRepository;

    @Mock
    private ShipmentTrackingService shipmentTrackingService;

    @InjectMocks
    private WarehouseMovementService warehouseMovementService;

    private Shipment sampleShipment;
    private Warehouse warehouseA;
    private Warehouse warehouseB;

    @BeforeEach
    void setUp() {
        sampleShipment = Shipment.builder()
                .id(10L)
                .trackingNumber("SHP-TEST-WH")
                .status(ShipmentStatus.IN_TRANSIT)
                .build();

        warehouseA = Warehouse.builder()
                .id(1L)
                .name("Cairo Hub")
                .city("Cairo")
                .status(WarehouseStatus.ACTIVE)
                .build();

        warehouseB = Warehouse.builder()
                .id(2L)
                .name("Alexandria Hub")
                .city("Alexandria")
                .status(WarehouseStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("moveShipmentToWarehouse creates first movement with null fromWarehouse")
    void moveShipmentToWarehouse_initialMovement_fromWarehouseNull() {
        when(shipmentRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(sampleShipment));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouseA));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(warehouseMovementRepository.save(any(WarehouseMovement.class))).thenAnswer(inv -> {
            WarehouseMovement m = inv.getArgument(0);
            m.setId(100L);
            m.setMovedAt(LocalDateTime.now());
            return m;
        });

        WarehouseMovementResponse response = warehouseMovementService.moveShipmentToWarehouse(10L, 1L, "First entry");

        assertNotNull(response);
        assertEquals(10L, response.shipmentId());
        assertNull(response.fromWarehouse());
        assertNotNull(response.toWarehouse());
        assertEquals("Cairo Hub", response.toWarehouse().name());
        assertEquals("First entry", response.notes());
        assertEquals(warehouseA, sampleShipment.getCurrentWarehouse());

        verify(shipmentRepository).save(sampleShipment);
        verify(warehouseMovementRepository).save(any(WarehouseMovement.class));
        verify(shipmentTrackingService).recordStatusChange(
                sampleShipment,
                ShipmentStatus.IN_TRANSIT,
                "Shipment moved to warehouse: Cairo Hub",
                "Cairo Hub"
        );
    }

    @Test
    @DisplayName("moveShipmentToWarehouse creates subsequent movement with previous fromWarehouse")
    void moveShipmentToWarehouse_subsequentMovement_fromWarehouseNotNull() {
        sampleShipment.setCurrentWarehouse(warehouseA);

        when(shipmentRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(sampleShipment));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(warehouseB));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(warehouseMovementRepository.save(any(WarehouseMovement.class))).thenAnswer(inv -> {
            WarehouseMovement m = inv.getArgument(0);
            m.setId(101L);
            m.setMovedAt(LocalDateTime.now());
            return m;
        });

        WarehouseMovementResponse response = warehouseMovementService.moveShipmentToWarehouse(10L, 2L, "Transfer to Alex");

        assertNotNull(response);
        assertEquals(10L, response.shipmentId());
        assertNotNull(response.fromWarehouse());
        assertEquals("Cairo Hub", response.fromWarehouse().name());
        assertNotNull(response.toWarehouse());
        assertEquals("Alexandria Hub", response.toWarehouse().name());
        assertEquals("Transfer to Alex", response.notes());
        assertEquals(warehouseB, sampleShipment.getCurrentWarehouse());

        verify(shipmentTrackingService).recordStatusChange(
                sampleShipment,
                ShipmentStatus.IN_TRANSIT,
                "Shipment moved to warehouse: Alexandria Hub",
                "Alexandria Hub"
        );
    }

    @Test
    @DisplayName("moveShipmentToWarehouse throws BadRequestException on null arguments")
    void moveShipmentToWarehouse_nullArguments_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> warehouseMovementService.moveShipmentToWarehouse(null, 1L, "notes"));
        assertThrows(BadRequestException.class, () -> warehouseMovementService.moveShipmentToWarehouse(10L, null, "notes"));
    }

    @Test
    @DisplayName("moveShipmentToWarehouse throws ResourceNotFoundException when shipment not found")
    void moveShipmentToWarehouse_shipmentNotFound_throwsResourceNotFoundException() {
        when(shipmentRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                warehouseMovementService.moveShipmentToWarehouse(999L, 1L, "notes"));
    }

    @Test
    @DisplayName("moveShipmentToWarehouse throws ResourceNotFoundException when warehouse not found")
    void moveShipmentToWarehouse_warehouseNotFound_throwsResourceNotFoundException() {
        when(shipmentRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(sampleShipment));
        when(warehouseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                warehouseMovementService.moveShipmentToWarehouse(10L, 999L, "notes"));
    }

    @ParameterizedTest(name = "Invalid warehouse status: {0}")
    @EnumSource(value = WarehouseStatus.class, names = {"INACTIVE", "MAINTENANCE"})
    @DisplayName("moveShipmentToWarehouse throws BadRequestException when warehouse is not ACTIVE")
    void moveShipmentToWarehouse_warehouseNotActive_throwsBadRequestException(WarehouseStatus status) {
        warehouseA.setStatus(status);
        when(shipmentRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(sampleShipment));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouseA));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                warehouseMovementService.moveShipmentToWarehouse(10L, 1L, "notes"));

        assertTrue(ex.getMessage().contains("Cannot move shipment to warehouse with status: " + status));
    }

    @Test
    @DisplayName("moveShipmentToWarehouse throws BadRequestException when moving to same current warehouse")
    void moveShipmentToWarehouse_sameWarehouse_throwsBadRequestException() {
        sampleShipment.setCurrentWarehouse(warehouseA);
        when(shipmentRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(sampleShipment));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouseA));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                warehouseMovementService.moveShipmentToWarehouse(10L, 1L, "notes"));

        assertTrue(ex.getMessage().contains("Shipment is already currently at warehouse: Cairo Hub"));
        verify(warehouseMovementRepository, never()).save(any());
    }

    @Test
    @DisplayName("getWarehouseMovements returns chronological movement list when shipment exists")
    void getWarehouseMovements_shipmentExists_returnsChronologicalList() {
        when(shipmentRepository.existsById(10L)).thenReturn(true);

        LocalDateTime now = LocalDateTime.now();
        WarehouseMovement m1 = WarehouseMovement.builder()
                .id(1L)
                .shipment(sampleShipment)
                .fromWarehouse(null)
                .toWarehouse(warehouseA)
                .notes("Origin entry")
                .movedAt(now.minusHours(3))
                .build();
        WarehouseMovement m2 = WarehouseMovement.builder()
                .id(2L)
                .shipment(sampleShipment)
                .fromWarehouse(warehouseA)
                .toWarehouse(warehouseB)
                .notes("Transit transfer")
                .movedAt(now.minusHours(1))
                .build();

        when(warehouseMovementRepository.findByShipmentIdOrderByMovedAtAsc(10L)).thenReturn(List.of(m1, m2));

        List<WarehouseMovementResponse> result = warehouseMovementService.getWarehouseMovements(10L);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Cairo Hub", result.get(0).toWarehouse().name());
        assertEquals("Alexandria Hub", result.get(1).toWarehouse().name());
    }

    @Test
    @DisplayName("getWarehouseMovements throws ResourceNotFoundException when shipment does not exist")
    void getWarehouseMovements_shipmentNotFound_throwsResourceNotFoundException() {
        when(shipmentRepository.existsById(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () ->
                warehouseMovementService.getWarehouseMovements(999L));
    }
}
