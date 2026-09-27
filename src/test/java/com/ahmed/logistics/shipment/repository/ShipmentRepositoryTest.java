package com.ahmed.logistics.shipment.repository;

import com.ahmed.logistics.customer.entity.Customer;
import com.ahmed.logistics.customer.repository.CustomerRepository;
import com.ahmed.logistics.shipment.entity.Shipment;
import com.ahmed.logistics.shipment.entity.ShipmentStatus;
import com.ahmed.logistics.shipment.entity.ShipmentType;
import com.ahmed.logistics.user.entity.Role;
import com.ahmed.logistics.user.entity.User;
import com.ahmed.logistics.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ShipmentRepositoryTest {

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    private Customer savedCustomer;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .email("shipment_repo_user@logistics.com")
                .password("$2a$10$hashedPass")
                .firstName("Shipment")
                .lastName("Customer")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();
        user = userRepository.saveAndFlush(user);

        Customer customer = Customer.builder()
                .user(user)
                .phone("+1555123456")
                .address("789 Industrial Way")
                .city("Chicago")
                .postalCode("60601")
                .build();
        savedCustomer = customerRepository.saveAndFlush(customer);
    }

    private Shipment buildSampleShipment(String trackingNumber) {
        return Shipment.builder()
                .trackingNumber(trackingNumber)
                .customer(savedCustomer)
                .status(ShipmentStatus.CREATED)
                .shipmentType(ShipmentType.STANDARD)
                .pickupAddress("100 Start Rd")
                .pickupCity("Chicago")
                .pickupPostalCode("60601")
                .deliveryAddress("200 End Ave")
                .deliveryCity("Detroit")
                .deliveryPostalCode("48201")
                .recipientName("Bob Recipient")
                .recipientPhone("+1555987654")
                .packageDescription("Machinery Spare Parts")
                .weightKg(12.5)
                .lengthCm(50.0)
                .widthCm(40.0)
                .heightCm(30.0)
                .basePrice(new BigDecimal("15.00"))
                .shippingFee(new BigDecimal("31.25"))
                .totalPrice(new BigDecimal("46.25"))
                .build();
    }

    @Test
    @DisplayName("saveAndFlush generates ID and timestamps")
    void saveAndFlush_persistsShipmentSuccessfully() {
        Shipment shipment = buildSampleShipment("SHP-REPO-001");

        Shipment saved = shipmentRepository.saveAndFlush(shipment);

        assertNotNull(saved.getId());
        assertEquals("SHP-REPO-001", saved.getTrackingNumber());
        assertEquals(ShipmentStatus.CREATED, saved.getStatus());
        assertEquals(savedCustomer.getId(), saved.getCustomer().getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    @DisplayName("findByTrackingNumber returns shipment when found")
    void findByTrackingNumber_returnsMatchingShipment() {
        Shipment shipment = buildSampleShipment("SHP-REPO-002");
        shipmentRepository.saveAndFlush(shipment);

        Optional<Shipment> found = shipmentRepository.findByTrackingNumber("SHP-REPO-002");

        assertTrue(found.isPresent());
        assertEquals("SHP-REPO-002", found.get().getTrackingNumber());
        assertEquals("Bob Recipient", found.get().getRecipientName());
    }

    @Test
    @DisplayName("existsByTrackingNumber returns true when exists and false otherwise")
    void existsByTrackingNumber_worksCorrectly() {
        Shipment shipment = buildSampleShipment("SHP-REPO-003");
        shipmentRepository.saveAndFlush(shipment);

        assertTrue(shipmentRepository.existsByTrackingNumber("SHP-REPO-003"));
        assertFalse(shipmentRepository.existsByTrackingNumber("SHP-NON-EXISTENT"));
    }

    @Test
    @DisplayName("findByCustomerId returns paginated shipments for customer")
    void findByCustomerId_returnsPaginatedResults() {
        Shipment s1 = buildSampleShipment("SHP-PAGE-001");
        Shipment s2 = buildSampleShipment("SHP-PAGE-002");
        shipmentRepository.saveAndFlush(s1);
        shipmentRepository.saveAndFlush(s2);

        Page<Shipment> page = shipmentRepository.findByCustomerId(savedCustomer.getId(), PageRequest.of(0, 10));

        assertNotNull(page);
        assertTrue(page.getTotalElements() >= 2);
    }

    @Test
    @DisplayName("duplicate tracking number violates unique constraint")
    void duplicateTrackingNumber_throwsException() {
        Shipment s1 = buildSampleShipment("SHP-DUPLICATE");
        shipmentRepository.saveAndFlush(s1);

        Shipment s2 = buildSampleShipment("SHP-DUPLICATE");

        assertThrows(DataIntegrityViolationException.class, () -> {
            shipmentRepository.saveAndFlush(s2);
        });
    }

    @Test
    @DisplayName("findByIdForUpdate returns shipment with pessimistic write lock")
    void findByIdForUpdate_returnsShipmentSuccessfully() {
        Shipment shipment = buildSampleShipment("SHP-FOR-UPDATE-001");
        Shipment saved = shipmentRepository.saveAndFlush(shipment);

        Optional<Shipment> locked = shipmentRepository.findByIdForUpdate(saved.getId());

        assertTrue(locked.isPresent());
        assertEquals("SHP-FOR-UPDATE-001", locked.get().getTrackingNumber());
    }
}
