package com.ahmed.logistics.shipment.service;

import com.ahmed.logistics.customer.entity.Customer;
import com.ahmed.logistics.exception.BadRequestException;
import com.ahmed.logistics.exception.ResourceNotFoundException;
import com.ahmed.logistics.shipment.dto.ShipmentResponse;
import com.ahmed.logistics.shipment.entity.Shipment;
import com.ahmed.logistics.shipment.entity.ShipmentStatus;
import com.ahmed.logistics.shipment.entity.ShipmentType;
import com.ahmed.logistics.shipment.repository.ShipmentRepository;
import com.ahmed.logistics.shipment.tracking.service.ShipmentTrackingService;
import com.ahmed.logistics.user.entity.Role;
import com.ahmed.logistics.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentLifecycleServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private ShipmentTrackingService shipmentTrackingService;

    @InjectMocks
    private ShipmentLifecycleService lifecycleService;

    private Shipment sampleShipment;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .id(1L)
                .email("cust@logistics.com")
                .firstName("John")
                .lastName("Doe")
                .role(Role.CUSTOMER)
                .build();

        Customer customer = Customer.builder()
                .id(10L)
                .user(user)
                .phone("+1555111222")
                .address("100 Main St")
                .city("Dallas")
                .postalCode("75001")
                .build();

        sampleShipment = Shipment.builder()
                .id(50L)
                .trackingNumber("SHP-LC-001")
                .customer(customer)
                .status(ShipmentStatus.CREATED)
                .shipmentType(ShipmentType.STANDARD)
                .pickupAddress("100 Main St")
                .pickupCity("Dallas")
                .pickupPostalCode("75001")
                .deliveryAddress("200 Commerce St")
                .deliveryCity("Austin")
                .deliveryPostalCode("78701")
                .recipientName("Alice Recipient")
                .recipientPhone("+1555333444")
                .weightKg(5.0)
                .basePrice(new BigDecimal("15.00"))
                .shippingFee(new BigDecimal("12.50"))
                .totalPrice(new BigDecimal("27.50"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @ParameterizedTest(name = "Valid transition from {0} to {1}")
    @CsvSource({
            "CREATED, CONFIRMED",
            "CREATED, CANCELLED",
            "CONFIRMED, PICKED_UP",
            "CONFIRMED, CANCELLED",
            "PICKED_UP, IN_TRANSIT",
            "IN_TRANSIT, OUT_FOR_DELIVERY",
            "OUT_FOR_DELIVERY, DELIVERED",
            "OUT_FOR_DELIVERY, DELIVERY_FAILED",
            "DELIVERY_FAILED, RESCHEDULED",
            "RESCHEDULED, OUT_FOR_DELIVERY",
            "DELIVERED, RETURNED"
    })
    @DisplayName("transitionStatus succeeds for all valid transitions in state machine")
    void transitionStatus_validTransitions_succeed(ShipmentStatus initialStatus, ShipmentStatus targetStatus) {
        sampleShipment.setStatus(initialStatus);

        when(shipmentRepository.findById(50L)).thenReturn(Optional.of(sampleShipment));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShipmentResponse response = lifecycleService.transitionStatus(50L, targetStatus);

        assertNotNull(response);
        assertEquals(targetStatus, response.status());
        assertEquals(targetStatus, sampleShipment.getStatus());
        verify(shipmentRepository).save(sampleShipment);
        verify(shipmentTrackingService).recordStatusChange(sampleShipment, targetStatus);
    }

    @ParameterizedTest(name = "Invalid transition from {0} to {1}")
    @CsvSource({
            "CREATED, DELIVERED",
            "CREATED, IN_TRANSIT",
            "CREATED, RETURNED",
            "CREATED, CREATED",
            "CONFIRMED, DELIVERED",
            "PICKED_UP, CREATED",
            "IN_TRANSIT, CONFIRMED",
            "OUT_FOR_DELIVERY, CREATED",
            "DELIVERED, IN_TRANSIT",
            "DELIVERED, CANCELLED",
            "CANCELLED, CONFIRMED",
            "CANCELLED, DELIVERED",
            "RETURNED, DELIVERED"
    })
    @DisplayName("transitionStatus throws BadRequestException for invalid transitions")
    void transitionStatus_invalidTransitions_throwBadRequestException(ShipmentStatus initialStatus, ShipmentStatus targetStatus) {
        sampleShipment.setStatus(initialStatus);

        when(shipmentRepository.findById(50L)).thenReturn(Optional.of(sampleShipment));

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> lifecycleService.transitionStatus(50L, targetStatus)
        );

        assertTrue(ex.getMessage().contains("Cannot transition shipment from " + initialStatus + " to " + targetStatus));
        verify(shipmentRepository, never()).save(any());
        verify(shipmentTrackingService, never()).recordStatusChange(any(), any());
    }

    @Test
    @DisplayName("transitionStatus throws BadRequestException when target status is null")
    void transitionStatus_nullTargetStatus_throwsBadRequestException() {
        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> lifecycleService.transitionStatus(50L, null)
        );

        assertEquals("Target shipment status must not be null", ex.getMessage());
        verify(shipmentRepository, never()).findById(any());
    }

    @Test
    @DisplayName("transitionStatus throws ResourceNotFoundException when shipment not found")
    void transitionStatus_shipmentNotFound_throwsResourceNotFoundException() {
        when(shipmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> lifecycleService.transitionStatus(999L, ShipmentStatus.CONFIRMED)
        );
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("isTransitionAllowed helper returns true for allowed and false for disallowed transitions")
    void isTransitionAllowed_checksCorrectly() {
        assertTrue(lifecycleService.isTransitionAllowed(ShipmentStatus.CREATED, ShipmentStatus.CONFIRMED));
        assertTrue(lifecycleService.isTransitionAllowed(ShipmentStatus.DELIVERED, ShipmentStatus.RETURNED));
        assertFalse(lifecycleService.isTransitionAllowed(ShipmentStatus.CREATED, ShipmentStatus.DELIVERED));
        assertFalse(lifecycleService.isTransitionAllowed(null, ShipmentStatus.CONFIRMED));
        assertFalse(lifecycleService.isTransitionAllowed(ShipmentStatus.CREATED, null));
    }
}
