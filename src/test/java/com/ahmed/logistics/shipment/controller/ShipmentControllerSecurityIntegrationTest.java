package com.ahmed.logistics.shipment.controller;

import com.ahmed.logistics.customer.entity.Customer;
import com.ahmed.logistics.customer.repository.CustomerRepository;
import com.ahmed.logistics.shipment.dto.CreateShipmentRequest;
import com.ahmed.logistics.shipment.dto.UpdateShipmentRequest;
import com.ahmed.logistics.shipment.entity.Shipment;
import com.ahmed.logistics.shipment.entity.ShipmentStatus;
import com.ahmed.logistics.shipment.entity.ShipmentType;
import com.ahmed.logistics.shipment.repository.ShipmentRepository;
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

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class ShipmentControllerSecurityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MockMvc mockMvc;

    private Customer customer1;
    private Customer customer2;
    private Shipment shipment1;
    private Shipment shipment2;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // User 1 (Customer 1)
        User user1 = userRepository.saveAndFlush(User.builder()
                .email("cust1_shp@logistics.com")
                .password("$2a$10$hashedPass")
                .firstName("Customer")
                .lastName("One")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build());

        customer1 = customerRepository.saveAndFlush(Customer.builder()
                .user(user1)
                .phone("+1555111111")
                .address("100 Elm St")
                .city("Dallas")
                .postalCode("75001")
                .build());

        // User 2 (Customer 2)
        User user2 = userRepository.saveAndFlush(User.builder()
                .email("cust2_shp@logistics.com")
                .password("$2a$10$hashedPass")
                .firstName("Customer")
                .lastName("Two")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build());

        customer2 = customerRepository.saveAndFlush(Customer.builder()
                .user(user2)
                .phone("+1555222222")
                .address("200 Oak St")
                .city("Houston")
                .postalCode("77001")
                .build());

        // User 3 (Admin)
        userRepository.saveAndFlush(User.builder()
                .email("admin_shp@logistics.com")
                .password("$2a$10$hashedPass")
                .firstName("Admin")
                .lastName("User")
                .role(Role.ADMIN)
                .enabled(true)
                .build());

        // User 4 (Dispatcher)
        userRepository.saveAndFlush(User.builder()
                .email("dispatcher_shp@logistics.com")
                .password("$2a$10$hashedPass")
                .firstName("Dispatcher")
                .lastName("User")
                .role(Role.DISPATCHER)
                .enabled(true)
                .build());

        // Seed Shipment 1 (Customer 1)
        shipment1 = shipmentRepository.saveAndFlush(Shipment.builder()
                .trackingNumber("SHP-CUST1-001")
                .customer(customer1)
                .status(ShipmentStatus.CREATED)
                .shipmentType(ShipmentType.STANDARD)
                .pickupAddress("100 Elm St")
                .pickupCity("Dallas")
                .pickupPostalCode("75001")
                .deliveryAddress("300 Main St")
                .deliveryCity("Austin")
                .deliveryPostalCode("78701")
                .recipientName("Alice Recipient")
                .recipientPhone("+1555333444")
                .packageDescription("Office Supplies")
                .weightKg(3.0)
                .lengthCm(20.0)
                .widthCm(20.0)
                .heightCm(10.0)
                .basePrice(new BigDecimal("15.00"))
                .shippingFee(new BigDecimal("7.50"))
                .totalPrice(new BigDecimal("22.50"))
                .build());

        // Seed Shipment 2 (Customer 2)
        shipment2 = shipmentRepository.saveAndFlush(Shipment.builder()
                .trackingNumber("SHP-CUST2-002")
                .customer(customer2)
                .status(ShipmentStatus.CREATED)
                .shipmentType(ShipmentType.EXPRESS)
                .pickupAddress("200 Oak St")
                .pickupCity("Houston")
                .pickupPostalCode("77001")
                .deliveryAddress("400 Commerce St")
                .deliveryCity("San Antonio")
                .deliveryPostalCode("78201")
                .recipientName("Bob Recipient")
                .recipientPhone("+1555555666")
                .packageDescription("Industrial Hardware")
                .weightKg(10.0)
                .lengthCm(40.0)
                .widthCm(30.0)
                .heightCm(20.0)
                .basePrice(new BigDecimal("25.00"))
                .shippingFee(new BigDecimal("50.00"))
                .totalPrice(new BigDecimal("75.00"))
                .build());
    }

    @Test
    @DisplayName("Unauthenticated request to shipments returns 401")
    void unauthenticatedRequest_returns401() throws Exception {
        mockMvc.perform(get("/api/shipments/" + shipment1.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("CUSTOMER can create shipment for themselves")
    @WithMockUser(username = "cust1_shp@logistics.com", roles = "CUSTOMER")
    void customer_canCreateOwnShipment() throws Exception {
        CreateShipmentRequest request = new CreateShipmentRequest(
                customer1.getId(),
                ShipmentType.STANDARD,
                "100 Elm St", "Dallas", "75001",
                "999 Target Rd", "Fort Worth", "76101",
                "Recipient One", "+1555777888",
                "Books", 2.0, 15.0, 15.0, 10.0
        );

        mockMvc.perform(post("/api/shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.trackingNumber").value(org.hamcrest.Matchers.startsWith("SHP-")))
                .andExpect(jsonPath("$.customerId").value(customer1.getId()))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    @DisplayName("CUSTOMER cannot create shipment for another customer")
    @WithMockUser(username = "cust1_shp@logistics.com", roles = "CUSTOMER")
    void customer_cannotCreateShipmentForAnotherCustomer() throws Exception {
        CreateShipmentRequest request = new CreateShipmentRequest(
                customer2.getId(), // attempting customer2's id
                ShipmentType.STANDARD,
                "100 Elm St", "Dallas", "75001",
                "999 Target Rd", "Fort Worth", "76101",
                "Recipient Imposter", "+1555777888",
                "Books", 2.0, 15.0, 15.0, 10.0
        );

        mockMvc.perform(post("/api/shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("ADMIN can create shipment for any customer")
    @WithMockUser(username = "admin_shp@logistics.com", roles = "ADMIN")
    void admin_canCreateShipmentForAnyCustomer() throws Exception {
        CreateShipmentRequest request = new CreateShipmentRequest(
                customer2.getId(),
                ShipmentType.EXPRESS,
                "200 Oak St", "Houston", "77001",
                "555 Corporate Dr", "Austin", "78701",
                "Corporate Recipient", "+1555000999",
                "Contracts", 1.5, 10.0, 10.0, 5.0
        );

        mockMvc.perform(post("/api/shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(customer2.getId()));
    }

    @Test
    @DisplayName("CUSTOMER can view own shipment by ID")
    @WithMockUser(username = "cust1_shp@logistics.com", roles = "CUSTOMER")
    void customer_canViewOwnShipmentById() throws Exception {
        mockMvc.perform(get("/api/shipments/" + shipment1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(shipment1.getId()))
                .andExpect(jsonPath("$.trackingNumber").value("SHP-CUST1-001"));
    }

    @Test
    @DisplayName("CUSTOMER cannot view another customer's shipment by ID")
    @WithMockUser(username = "cust1_shp@logistics.com", roles = "CUSTOMER")
    void customer_cannotViewAnotherCustomerShipmentById() throws Exception {
        mockMvc.perform(get("/api/shipments/" + shipment2.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("ADMIN can view any shipment by ID")
    @WithMockUser(username = "admin_shp@logistics.com", roles = "ADMIN")
    void admin_canViewAnyShipmentById() throws Exception {
        mockMvc.perform(get("/api/shipments/" + shipment2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(shipment2.getId()));
    }

    @Test
    @DisplayName("DISPATCHER can view any shipment by ID")
    @WithMockUser(username = "dispatcher_shp@logistics.com", roles = "DISPATCHER")
    void dispatcher_canViewAnyShipmentById() throws Exception {
        mockMvc.perform(get("/api/shipments/" + shipment1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(shipment1.getId()));
    }

    @Test
    @DisplayName("CUSTOMER can view own shipment by tracking number")
    @WithMockUser(username = "cust1_shp@logistics.com", roles = "CUSTOMER")
    void customer_canViewOwnShipmentByTrackingNumber() throws Exception {
        mockMvc.perform(get("/api/shipments/tracking/" + shipment1.getTrackingNumber()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingNumber").value("SHP-CUST1-001"));
    }

    @Test
    @DisplayName("CUSTOMER cannot view another customer's shipment by tracking number")
    @WithMockUser(username = "cust1_shp@logistics.com", roles = "CUSTOMER")
    void customer_cannotViewAnotherCustomerShipmentByTrackingNumber() throws Exception {
        mockMvc.perform(get("/api/shipments/tracking/" + shipment2.getTrackingNumber()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("CUSTOMER can view own customer shipments")
    @WithMockUser(username = "cust1_shp@logistics.com", roles = "CUSTOMER")
    void customer_canViewOwnCustomerShipments() throws Exception {
        mockMvc.perform(get("/api/shipments/customer/" + customer1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].trackingNumber").value("SHP-CUST1-001"));
    }

    @Test
    @DisplayName("CUSTOMER cannot view another customer's shipment list")
    @WithMockUser(username = "cust1_shp@logistics.com", roles = "CUSTOMER")
    void customer_cannotViewAnotherCustomerShipmentList() throws Exception {
        mockMvc.perform(get("/api/shipments/customer/" + customer2.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("CUSTOMER can update own shipment")
    @WithMockUser(username = "cust1_shp@logistics.com", roles = "CUSTOMER")
    void customer_canUpdateOwnShipment() throws Exception {
        UpdateShipmentRequest updateRequest = new UpdateShipmentRequest(
                ShipmentType.EXPRESS,
                "100 Elm St Updated", "Dallas", "75001",
                "300 Main St Updated", "Austin", "78701",
                "Alice Updated", "+1555333444",
                "Updated Description", 5.0, 25.0, 25.0, 15.0
        );

        mockMvc.perform(put("/api/shipments/" + shipment1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pickupAddress").value("100 Elm St Updated"))
                .andExpect(jsonPath("$.shipmentType").value("EXPRESS"));
    }

    @Test
    @DisplayName("CUSTOMER cannot update another customer's shipment")
    @WithMockUser(username = "cust1_shp@logistics.com", roles = "CUSTOMER")
    void customer_cannotUpdateAnotherCustomerShipment() throws Exception {
        UpdateShipmentRequest updateRequest = new UpdateShipmentRequest(
                ShipmentType.EXPRESS,
                "Hacked Address", "Dallas", "75001",
                "Hacked Delivery", "Austin", "78701",
                "Hacker", "+1555333444",
                "Updated Description", 5.0, 25.0, 25.0, 15.0
        );

        mockMvc.perform(put("/api/shipments/" + shipment2.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("CUSTOMER can delete own shipment")
    @WithMockUser(username = "cust1_shp@logistics.com", roles = "CUSTOMER")
    void customer_canDeleteOwnShipment() throws Exception {
        mockMvc.perform(delete("/api/shipments/" + shipment1.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("CUSTOMER cannot delete another customer's shipment")
    @WithMockUser(username = "cust1_shp@logistics.com", roles = "CUSTOMER")
    void customer_cannotDeleteAnotherCustomerShipment() throws Exception {
        mockMvc.perform(delete("/api/shipments/" + shipment2.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Validation fails when required fields are missing")
    @WithMockUser(username = "cust1_shp@logistics.com", roles = "CUSTOMER")
    void createShipment_validationFails() throws Exception {
        CreateShipmentRequest invalidRequest = new CreateShipmentRequest(
                customer1.getId(),
                null,
                "", "", "", "", "", "", "", "", "", -1.0, -1.0, -1.0, -1.0
        );

        mockMvc.perform(post("/api/shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.shipmentType").isNotEmpty())
                .andExpect(jsonPath("$.pickupAddress").isNotEmpty())
                .andExpect(jsonPath("$.weightKg").isNotEmpty());
    }
}
