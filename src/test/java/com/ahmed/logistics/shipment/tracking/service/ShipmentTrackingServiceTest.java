package com.ahmed.logistics.shipment.tracking.service;

import com.ahmed.logistics.exception.ResourceNotFoundException;
import com.ahmed.logistics.shipment.entity.Shipment;
import com.ahmed.logistics.shipment.entity.ShipmentStatus;
import com.ahmed.logistics.shipment.repository.ShipmentRepository;
import com.ahmed.logistics.shipment.tracking.dto.ShipmentTrackingResponse;
import com.ahmed.logistics.shipment.tracking.entity.ShipmentTracking;
import com.ahmed.logistics.shipment.tracking.repository.ShipmentTrackingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentTrackingServiceTest {

    @Mock
    private ShipmentTrackingRepository shipmentTrackingRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @InjectMocks
    private ShipmentTrackingService shipmentTrackingService;

    private Shipment sampleShipment;

    @BeforeEach
    void setUp() {
        sampleShipment = Shipment.builder()
                .id(100L)
                .trackingNumber("SHP-TRACK123")
                .status(ShipmentStatus.CREATED)
                .build();
    }

    @Test
    @DisplayName("recordStatusChange with custom description and location saves tracking record")
    void recordStatusChange_customDescriptionAndLocation_savesTracking() {
        shipmentTrackingService.recordStatusChange(sampleShipment, ShipmentStatus.PICKED_UP, "Package picked up from origin", "New York Hub");

        ArgumentCaptor<ShipmentTracking> captor = ArgumentCaptor.forClass(ShipmentTracking.class);
        verify(shipmentTrackingRepository).save(captor.capture());

        ShipmentTracking saved = captor.getValue();
        assertEquals(sampleShipment, saved.getShipment());
        assertEquals(ShipmentStatus.PICKED_UP, saved.getStatus());
        assertEquals("Package picked up from origin", saved.getDescription());
        assertEquals("New York Hub", saved.getLocation());
    }

    @Test
    @DisplayName("recordStatusChange with null/blank description uses default description")
    void recordStatusChange_defaultDescription_usesCentralizedMapping() {
        shipmentTrackingService.recordStatusChange(sampleShipment, ShipmentStatus.CONFIRMED);

        ArgumentCaptor<ShipmentTracking> captor = ArgumentCaptor.forClass(ShipmentTracking.class);
        verify(shipmentTrackingRepository).save(captor.capture());

        ShipmentTracking saved = captor.getValue();
        assertEquals(sampleShipment, saved.getShipment());
        assertEquals(ShipmentStatus.CONFIRMED, saved.getStatus());
        assertEquals("Shipment confirmed", saved.getDescription());
        assertNull(saved.getLocation());
    }

    @Test
    @DisplayName("recordStatusChange throws IllegalArgumentException when shipment is null")
    void recordStatusChange_nullShipment_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> shipmentTrackingService.recordStatusChange(null, ShipmentStatus.CREATED)
        );

        assertEquals("Shipment must not be null when recording tracking status", ex.getMessage());
        verify(shipmentTrackingRepository, never()).save(any());
    }

    @Test
    @DisplayName("recordStatusChange throws IllegalArgumentException when status is null")
    void recordStatusChange_nullStatus_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> shipmentTrackingService.recordStatusChange(sampleShipment, null)
        );

        assertEquals("Status must not be null when recording tracking status", ex.getMessage());
        verify(shipmentTrackingRepository, never()).save(any());
    }

    @Test
    @DisplayName("getDefaultDescription returns centralized descriptions for all statuses")
    void getDefaultDescription_returnsExpectedDescriptions() {
        assertEquals("Shipment created", shipmentTrackingService.getDefaultDescription(ShipmentStatus.CREATED));
        assertEquals("Shipment confirmed", shipmentTrackingService.getDefaultDescription(ShipmentStatus.CONFIRMED));
        assertEquals("Shipment picked up", shipmentTrackingService.getDefaultDescription(ShipmentStatus.PICKED_UP));
        assertEquals("Shipment is in transit", shipmentTrackingService.getDefaultDescription(ShipmentStatus.IN_TRANSIT));
        assertEquals("Shipment is out for delivery", shipmentTrackingService.getDefaultDescription(ShipmentStatus.OUT_FOR_DELIVERY));
        assertEquals("Shipment delivered", shipmentTrackingService.getDefaultDescription(ShipmentStatus.DELIVERED));
        assertEquals("Shipment cancelled", shipmentTrackingService.getDefaultDescription(ShipmentStatus.CANCELLED));
        assertEquals("Delivery attempt failed", shipmentTrackingService.getDefaultDescription(ShipmentStatus.DELIVERY_FAILED));
        assertEquals("Shipment rescheduled", shipmentTrackingService.getDefaultDescription(ShipmentStatus.RESCHEDULED));
        assertEquals("Shipment returned", shipmentTrackingService.getDefaultDescription(ShipmentStatus.RETURNED));
        assertEquals("Status updated", shipmentTrackingService.getDefaultDescription(null));
    }

    @Test
    @DisplayName("getShipmentTracking returns ordered timeline responses when shipment exists")
    void getShipmentTracking_shipmentExists_returnsOrderedTimeline() {
        when(shipmentRepository.existsById(100L)).thenReturn(true);

        LocalDateTime now = LocalDateTime.now();
        ShipmentTracking t1 = ShipmentTracking.builder()
                .id(1L)
                .shipment(sampleShipment)
                .status(ShipmentStatus.CREATED)
                .description("Shipment created")
                .createdAt(now.minusHours(2))
                .build();
        ShipmentTracking t2 = ShipmentTracking.builder()
                .id(2L)
                .shipment(sampleShipment)
                .status(ShipmentStatus.CONFIRMED)
                .description("Shipment confirmed")
                .createdAt(now.minusHours(1))
                .build();

        when(shipmentTrackingRepository.findByShipmentIdOrderByCreatedAtAsc(100L)).thenReturn(List.of(t1, t2));

        List<ShipmentTrackingResponse> result = shipmentTrackingService.getShipmentTracking(100L);

        assertNotNull(result);
        assertEquals(2, result.size());

        assertEquals(1L, result.get(0).id());
        assertEquals(100L, result.get(0).shipmentId());
        assertEquals(ShipmentStatus.CREATED, result.get(0).status());
        assertEquals("Shipment created", result.get(0).description());

        assertEquals(2L, result.get(1).id());
        assertEquals(100L, result.get(1).shipmentId());
        assertEquals(ShipmentStatus.CONFIRMED, result.get(1).status());
        assertEquals("Shipment confirmed", result.get(1).description());

        verify(shipmentTrackingRepository).findByShipmentIdOrderByCreatedAtAsc(100L);
    }

    @Test
    @DisplayName("getShipmentTracking throws ResourceNotFoundException when shipment does not exist")
    void getShipmentTracking_shipmentDoesNotExist_throwsResourceNotFoundException() {
        when(shipmentRepository.existsById(999L)).thenReturn(false);

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> shipmentTrackingService.getShipmentTracking(999L)
        );

        assertTrue(ex.getMessage().contains("Shipment not found with ID: 999"));
        verify(shipmentTrackingRepository, never()).findByShipmentIdOrderByCreatedAtAsc(any());
    }
}
