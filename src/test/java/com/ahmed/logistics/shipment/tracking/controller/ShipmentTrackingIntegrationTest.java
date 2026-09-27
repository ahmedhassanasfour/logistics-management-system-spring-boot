package com.ahmed.logistics.shipment.tracking.controller;

import com.ahmed.logistics.customer.entity.Customer;
import com.ahmed.logistics.customer.repository.CustomerRepository;
import com.ahmed.logistics.shipment.dto.CreateShipmentRequest;
import com.ahmed.logistics.shipment.dto.ShipmentResponse;
import com.ahmed.logistics.shipment.dto.UpdateShipmentStatusRequest;
import com.ahmed.logistics.shipment.entity.Shipment;
import com.ahmed.logistics.shipment.entity.ShipmentStatus;
import com.ahmed.logistics.shipment.entity.ShipmentType;
import com.ahmed.logistics.shipment.repository.ShipmentRepository;
import com.ahmed.logistics.shipment.service.ShipmentLifecycleService;
import com.ahmed.logistics.shipment.service.ShipmentService;
import com.ahmed.logistics.shipment.tracking.entity.ShipmentTracking;
import com.ahmed.logistics.shipment.tracking.repository.ShipmentTrackingRepository;
import com.ahmed.logistics.user.entity.Role;
import com.ahmed.logistics.user.entity.User;
import com.ahmed.logistics.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class ShipmentTrackingIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private ShipmentTrackingRepository shipmentTrackingRepository;

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private ShipmentLifecycleService shipmentLifecycleService;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MockMvc mockMvc;

    private Customer customer1;
    private Customer customer2;
    private Shipment shipment1;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Customer 1
        User user1 = userRepository.saveAndFlush(User.builder()
                .email("trk_cust1@logistics.com")
                .password("$2a$10$hashedPass")
                .firstName("Customer")
                .lastName("One")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build());

        customer1 = customerRepository.saveAndFlush(Customer.builder()
                .user(user1)
                .phone("+1234567891")
                .address("100 Main St")
                .city("Boston")
                .postalCode("02101")
                .build());

        // Customer 2
        User user2 = userRepository.saveAndFlush(User.builder()
                .email("trk_cust2@logistics.com")
                .password("$2a$10$hashedPass")
                .firstName("Customer")
                .lastName("Two")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build());

        customer2 = customerRepository.saveAndFlush(Customer.builder()
                .user(user2)
                .phone("+1234567892")
                .address("200 Main St")
                .city("Chicago")
                .postalCode("60601")
                .build());

        // Create Shipment via shipmentService so initial CREATED tracking is recorded
        ShipmentResponse created = shipmentService.createShipment(new CreateShipmentRequest(
                customer1.getId(),
                ShipmentType.STANDARD,
                "100 Main St",
                "Boston",
                "02101",
                "500 Elm St",
                "New York",
                "10001",
                "Alice Smith",
                "+1234567890",
                "Documents",
                2.5,
                10.0,
                10.0,
                10.0
        ));

        shipment1 = shipmentRepository.findById(created.id()).orElseThrow();
    }

    @Test
    @DisplayName("Shipment creation creates initial CREATED tracking record")
    @WithMockUser(username = "trk_cust1@logistics.com", roles = {"CUSTOMER"})
    void shipmentCreation_createsInitialCreatedTracking() throws Exception {
        List<ShipmentTracking> records = shipmentTrackingRepository.findByShipmentIdOrderByCreatedAtAsc(shipment1.getId());
        assertEquals(1, records.size());
        assertEquals(ShipmentStatus.CREATED, records.get(0).getStatus());
        assertEquals("Shipment created", records.get(0).getDescription());

        mockMvc.perform(get("/api/shipments/{shipmentId}/tracking", shipment1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].shipmentId").value(shipment1.getId()))
                .andExpect(jsonPath("$[0].status").value("CREATED"))
                .andExpect(jsonPath("$[0].description").value("Shipment created"));
    }

    @Test
    @DisplayName("Valid status transitions append tracking events in chronological order")
    @WithMockUser(username = "admin@logistics.com", roles = {"ADMIN"})
    void statusTransitions_appendTrackingEventsChronologically() throws Exception {
        // CREATED -> CONFIRMED
        mockMvc.perform(patch("/api/shipments/{id}/status", shipment1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateShipmentStatusRequest(ShipmentStatus.CONFIRMED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // CONFIRMED -> PICKED_UP
        mockMvc.perform(patch("/api/shipments/{id}/status", shipment1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateShipmentStatusRequest(ShipmentStatus.PICKED_UP))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PICKED_UP"));

        // Verify tracking timeline has 3 records: CREATED, CONFIRMED, PICKED_UP
        mockMvc.perform(get("/api/shipments/{shipmentId}/tracking", shipment1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].status").value("CREATED"))
                .andExpect(jsonPath("$[0].description").value("Shipment created"))
                .andExpect(jsonPath("$[1].status").value("CONFIRMED"))
                .andExpect(jsonPath("$[1].description").value("Shipment confirmed"))
                .andExpect(jsonPath("$[2].status").value("PICKED_UP"))
                .andExpect(jsonPath("$[2].description").value("Shipment picked up"));
    }

    @Test
    @DisplayName("Invalid status transition does not create tracking record")
    @WithMockUser(username = "admin@logistics.com", roles = {"ADMIN"})
    void invalidTransition_doesNotCreateTracking() throws Exception {
        // Attempt invalid CREATED -> DELIVERED
        mockMvc.perform(patch("/api/shipments/{id}/status", shipment1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateShipmentStatusRequest(ShipmentStatus.DELIVERED))))
                .andExpect(status().isBadRequest());

        // Still only 1 tracking record (CREATED)
        List<ShipmentTracking> records = shipmentTrackingRepository.findByShipmentIdOrderByCreatedAtAsc(shipment1.getId());
        assertEquals(1, records.size());
        assertEquals(ShipmentStatus.CREATED, records.get(0).getStatus());
    }

    @Test
    @DisplayName("ADMIN role can view tracking for any shipment")
    @WithMockUser(username = "admin@logistics.com", roles = {"ADMIN"})
    void getTracking_asAdmin_returns200() throws Exception {
        mockMvc.perform(get("/api/shipments/{shipmentId}/tracking", shipment1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("DISPATCHER role can view tracking for any shipment")
    @WithMockUser(username = "disp@logistics.com", roles = {"DISPATCHER"})
    void getTracking_asDispatcher_returns200() throws Exception {
        mockMvc.perform(get("/api/shipments/{shipmentId}/tracking", shipment1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("Shipment owner customer can view tracking")
    @WithMockUser(username = "trk_cust1@logistics.com", roles = {"CUSTOMER"})
    void getTracking_asOwner_returns200() throws Exception {
        mockMvc.perform(get("/api/shipments/{shipmentId}/tracking", shipment1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("Non-owner customer cannot view tracking (returns 403)")
    @WithMockUser(username = "trk_cust2@logistics.com", roles = {"CUSTOMER"})
    void getTracking_asOtherCustomer_returns403() throws Exception {
        mockMvc.perform(get("/api/shipments/{shipmentId}/tracking", shipment1.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DRIVER cannot view tracking directly (returns 403)")
    @WithMockUser(username = "driver@logistics.com", roles = {"DRIVER"})
    void getTracking_asDriver_returns403() throws Exception {
        mockMvc.perform(get("/api/shipments/{shipmentId}/tracking", shipment1.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request to tracking returns 401")
    void getTracking_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/shipments/{shipmentId}/tracking", shipment1.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Non-existent shipment ID returns 404 for admin")
    @WithMockUser(username = "admin@logistics.com", roles = {"ADMIN"})
    void getTracking_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/shipments/{shipmentId}/tracking", 999999L))
                .andExpect(status().isNotFound());
    }
}
