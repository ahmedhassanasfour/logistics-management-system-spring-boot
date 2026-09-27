package com.ahmed.logistics.shipment.service;

import com.ahmed.logistics.customer.entity.Customer;
import com.ahmed.logistics.customer.repository.CustomerRepository;
import com.ahmed.logistics.exception.BadRequestException;
import com.ahmed.logistics.exception.ResourceNotFoundException;
import com.ahmed.logistics.shipment.dto.CreateShipmentRequest;
import com.ahmed.logistics.shipment.dto.ShipmentResponse;
import com.ahmed.logistics.shipment.dto.UpdateShipmentRequest;
import com.ahmed.logistics.shipment.entity.Shipment;
import com.ahmed.logistics.shipment.entity.ShipmentStatus;
import com.ahmed.logistics.shipment.entity.ShipmentType;
import com.ahmed.logistics.shipment.pricing.ShipmentPricingResponse;
import com.ahmed.logistics.shipment.pricing.ShipmentPricingService;
import com.ahmed.logistics.shipment.repository.ShipmentRepository;
import com.ahmed.logistics.shipment.tracking.service.ShipmentTrackingService;
import com.ahmed.logistics.user.entity.Role;
import com.ahmed.logistics.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private TrackingNumberGenerator trackingNumberGenerator;

    @Mock
    private ShipmentPricingService pricingService;

    @Mock
    private ShipmentTrackingService shipmentTrackingService;

    @InjectMocks
    private ShipmentService shipmentService;

    private Customer customer;
    private User user;
    private Shipment sampleShipment;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test.customer@logistics.com")
                .firstName("Test")
                .lastName("Customer")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();

        customer = Customer.builder()
                .id(10L)
                .user(user)
                .phone("+1555000111")
                .address("500 Logistics Blvd")
                .city("Atlanta")
                .postalCode("30301")
                .build();

        sampleShipment = Shipment.builder()
                .id(100L)
                .trackingNumber("SHP-TEST1234")
                .customer(customer)
                .status(ShipmentStatus.CREATED)
                .shipmentType(ShipmentType.STANDARD)
                .pickupAddress("500 Logistics Blvd")
                .pickupCity("Atlanta")
                .pickupPostalCode("30301")
                .deliveryAddress("600 Commercial Ave")
                .deliveryCity("Miami")
                .deliveryPostalCode("33101")
                .recipientName("Jane Recipient")
                .recipientPhone("+1555999888")
                .packageDescription("Textiles")
                .weightKg(5.0)
                .lengthCm(25.0)
                .widthCm(25.0)
                .heightCm(25.0)
                .basePrice(new BigDecimal("15.00"))
                .shippingFee(new BigDecimal("12.50"))
                .totalPrice(new BigDecimal("27.50"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("createShipment saves shipment and calculates pricing")
    void createShipment_success() {
        CreateShipmentRequest request = new CreateShipmentRequest(
                10L,
                ShipmentType.STANDARD,
                "500 Logistics Blvd",
                "Atlanta",
                "30301",
                "600 Commercial Ave",
                "Miami",
                "33101",
                "Jane Recipient",
                "+1555999888",
                "Textiles",
                5.0,
                25.0,
                25.0,
                25.0
        );

        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(trackingNumberGenerator.generate()).thenReturn("SHP-GEN12345");
        when(shipmentRepository.existsByTrackingNumber("SHP-GEN12345")).thenReturn(false);
        when(pricingService.calculatePrice(ShipmentType.STANDARD, 5.0))
                .thenReturn(new ShipmentPricingResponse(
                        new BigDecimal("15.00"),
                        new BigDecimal("12.50"),
                        new BigDecimal("27.50")
                ));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(sampleShipment);

        ShipmentResponse response = shipmentService.createShipment(request);

        assertNotNull(response);
        assertEquals(100L, response.id());
        verify(shipmentRepository).save(any(Shipment.class));
        verify(shipmentTrackingService).recordStatusChange(sampleShipment, ShipmentStatus.CREATED);
    }

    @Test
    @DisplayName("createShipment throws ResourceNotFoundException when customer not found")
    void createShipment_customerNotFound() {
        CreateShipmentRequest request = new CreateShipmentRequest(
                999L,
                ShipmentType.STANDARD,
                "A", "B", "C", "D", "E", "F", "G", "H", "I", 1.0, 1.0, 1.0, 1.0
        );

        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> shipmentService.createShipment(request));
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("createShipment throws BadRequestException when customer has invalid role")
    void createShipment_invalidRole() {
        User driverUser = User.builder().id(2L).role(Role.DRIVER).build();
        Customer driverCustomer = Customer.builder().id(20L).user(driverUser).build();

        CreateShipmentRequest request = new CreateShipmentRequest(
                20L,
                ShipmentType.STANDARD,
                "A", "B", "C", "D", "E", "F", "G", "H", "I", 1.0, 1.0, 1.0, 1.0
        );

        when(customerRepository.findById(20L)).thenReturn(Optional.of(driverCustomer));

        assertThrows(BadRequestException.class, () -> shipmentService.createShipment(request));
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("getShipmentById returns shipment response")
    void getShipmentById_success() {
        when(shipmentRepository.findById(100L)).thenReturn(Optional.of(sampleShipment));

        ShipmentResponse response = shipmentService.getShipmentById(100L);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals("SHP-TEST1234", response.trackingNumber());
    }

    @Test
    @DisplayName("getShipmentById throws ResourceNotFoundException when not found")
    void getShipmentById_notFound() {
        when(shipmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> shipmentService.getShipmentById(999L));
    }

    @Test
    @DisplayName("getShipmentByTrackingNumber returns shipment response")
    void getShipmentByTrackingNumber_success() {
        when(shipmentRepository.findByTrackingNumber("SHP-TEST1234")).thenReturn(Optional.of(sampleShipment));

        ShipmentResponse response = shipmentService.getShipmentByTrackingNumber("SHP-TEST1234");

        assertNotNull(response);
        assertEquals("SHP-TEST1234", response.trackingNumber());
    }

    @Test
    @DisplayName("getShipmentByTrackingNumber throws ResourceNotFoundException when not found")
    void getShipmentByTrackingNumber_notFound() {
        when(shipmentRepository.findByTrackingNumber("SHP-UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> shipmentService.getShipmentByTrackingNumber("SHP-UNKNOWN"));
    }

    @Test
    @DisplayName("getCustomerShipments returns paginated response")
    void getCustomerShipments_success() {
        Pageable pageable = PageRequest.of(0, 10);
        when(customerRepository.existsById(10L)).thenReturn(true);
        when(shipmentRepository.findByCustomerId(10L, pageable))
                .thenReturn(new PageImpl<>(List.of(sampleShipment), pageable, 1));

        Page<ShipmentResponse> result = shipmentService.getCustomerShipments(10L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("updateShipment modifies shipment and updates price")
    void updateShipment_success() {
        UpdateShipmentRequest updateRequest = new UpdateShipmentRequest(
                ShipmentType.EXPRESS,
                "New Pickup", "Atlanta", "30301",
                "New Delivery", "Orlando", "32801",
                "New Recipient", "+1555222333",
                "Fragile Goods", 8.0, 30.0, 30.0, 30.0
        );

        when(shipmentRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(sampleShipment));
        when(pricingService.calculatePrice(ShipmentType.EXPRESS, 8.0))
                .thenReturn(new ShipmentPricingResponse(
                        new BigDecimal("25.00"),
                        new BigDecimal("40.00"),
                        new BigDecimal("65.00")
                ));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(sampleShipment);

        ShipmentResponse updated = shipmentService.updateShipment(100L, updateRequest);

        assertNotNull(updated);
        verify(pricingService).calculatePrice(ShipmentType.EXPRESS, 8.0);
        verify(shipmentRepository).save(sampleShipment);
    }

    @Test
    @DisplayName("updateShipment with unchanged type and weight does not recalculate pricing")
    void updateShipment_unchangedPricingFields_doesNotRecalculatePricing() {
        UpdateShipmentRequest updateRequest = new UpdateShipmentRequest(
                ShipmentType.STANDARD, // same as sampleShipment
                "New Pickup", "Atlanta", "30301",
                "New Delivery", "Orlando", "32801",
                "New Recipient", "+1555222333",
                "Updated Description",
                5.0, // same as sampleShipment
                30.0, 30.0, 30.0
        );

        when(shipmentRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(sampleShipment));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(sampleShipment);

        ShipmentResponse updated = shipmentService.updateShipment(100L, updateRequest);

        assertNotNull(updated);
        verify(pricingService, never()).calculatePrice(any(ShipmentType.class), any(Double.class));
        verify(pricingService, never()).calculatePrice(any(ShipmentType.class), any(BigDecimal.class));
        verify(shipmentRepository).save(sampleShipment);
    }

    @Test
    @DisplayName("deleteShipment deletes shipment by ID")
    void deleteShipment_success() {
        when(shipmentRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(sampleShipment));

        shipmentService.deleteShipment(100L);

        verify(shipmentRepository).delete(sampleShipment);
    }
}
