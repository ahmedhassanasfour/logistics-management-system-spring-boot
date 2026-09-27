package com.ahmed.logistics.warehouse.movement.controller;

import com.ahmed.logistics.customer.entity.Customer;
import com.ahmed.logistics.customer.repository.CustomerRepository;
import com.ahmed.logistics.shipment.dto.CreateShipmentRequest;
import com.ahmed.logistics.shipment.dto.ShipmentResponse;
import com.ahmed.logistics.shipment.entity.Shipment;
import com.ahmed.logistics.shipment.entity.ShipmentType;
import com.ahmed.logistics.shipment.repository.ShipmentRepository;
import com.ahmed.logistics.shipment.service.ShipmentService;
import com.ahmed.logistics.shipment.tracking.entity.ShipmentTracking;
import com.ahmed.logistics.shipment.tracking.repository.ShipmentTrackingRepository;
import com.ahmed.logistics.user.entity.Role;
import com.ahmed.logistics.user.entity.User;
import com.ahmed.logistics.user.repository.UserRepository;
import com.ahmed.logistics.warehouse.entity.Warehouse;
import com.ahmed.logistics.warehouse.entity.WarehouseStatus;
import com.ahmed.logistics.warehouse.movement.dto.MoveShipmentToWarehouseRequest;
import com.ahmed.logistics.warehouse.movement.entity.WarehouseMovement;
import com.ahmed.logistics.warehouse.movement.repository.WarehouseMovementRepository;
import com.ahmed.logistics.warehouse.repository.WarehouseRepository;
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

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class WarehouseMovementIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private WarehouseMovementRepository warehouseMovementRepository;

    @Autowired
    private ShipmentTrackingRepository shipmentTrackingRepository;

    @Autowired
    private ShipmentService shipmentService;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MockMvc mockMvc;

    private Customer customer1;
    private Customer customer2;
    private Warehouse warehouseCairo;
    private Warehouse warehouseAlex;
    private Warehouse warehouseInactive;
    private Shipment shipment;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Customer 1
        User user1 = userRepository.saveAndFlush(User.builder()
                .email("wm_cust1@logistics.com")
                .password("$2a$10$hashedPass")
                .firstName("Customer")
                .lastName("One")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build());

        customer1 = customerRepository.saveAndFlush(Customer.builder()
                .user(user1)
                .phone("+201001111111")
                .address("Tahrir Sq")
                .city("Cairo")
                .postalCode("11511")
                .build());

        // Customer 2
        User user2 = userRepository.saveAndFlush(User.builder()
                .email("wm_cust2@logistics.com")
                .password("$2a$10$hashedPass")
                .firstName("Customer")
                .lastName("Two")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build());

        customer2 = customerRepository.saveAndFlush(Customer.builder()
                .user(user2)
                .phone("+201002222222")
                .address("Corniche")
                .city("Alexandria")
                .postalCode("21500")
                .build());

        // Warehouses
        warehouseCairo = warehouseRepository.saveAndFlush(Warehouse.builder()
                .name("Cairo Central Hub")
                .address("Ring Rd")
                .city("Cairo")
                .postalCode("11511")
                .phone("+2022000001")
                .email("cairo@logistics.com")
                .status(WarehouseStatus.ACTIVE)
                .build());

        warehouseAlex = warehouseRepository.saveAndFlush(Warehouse.builder()
                .name("Alexandria Port Hub")
                .address("Port St")
                .city("Alexandria")
                .postalCode("21500")
                .phone("+2033000002")
                .email("alex@logistics.com")
                .status(WarehouseStatus.ACTIVE)
                .build());

        warehouseInactive = warehouseRepository.saveAndFlush(Warehouse.builder()
                .name("Giza Maintenance Hub")
                .address("Pyramids St")
                .city("Giza")
                .postalCode("12511")
                .phone("+2022000003")
                .email("giza@logistics.com")
                .status(WarehouseStatus.MAINTENANCE)
                .build());

        // Shipment
        ShipmentResponse created = shipmentService.createShipment(new CreateShipmentRequest(
                customer1.getId(),
                ShipmentType.STANDARD,
                "Tahrir Sq",
                "Cairo",
                "11511",
                "Corniche",
                "Alexandria",
                "21500",
                "Recipient",
                "+201003333333",
                "Electronics",
                10.0,
                20.0,
                20.0,
                20.0
        ));

        shipment = shipmentRepository.findById(created.id()).orElseThrow();
    }

    @Test
    @DisplayName("Move shipment updates warehouse, creates movement record, and appends tracking event")
    @WithMockUser(username = "admin@logistics.com", roles = {"ADMIN"})
    void moveShipment_initialAndSubsequentMovements_success() throws Exception {
        // 1. Initial movement to Cairo Central Hub
        MoveShipmentToWarehouseRequest req1 = new MoveShipmentToWarehouseRequest(warehouseCairo.getId(), "Received at Cairo");

        mockMvc.perform(patch("/api/shipments/{shipmentId}/warehouse", shipment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipmentId").value(shipment.getId()))
                .andExpect(jsonPath("$.fromWarehouse", nullValue()))
                .andExpect(jsonPath("$.toWarehouse.id").value(warehouseCairo.getId()))
                .andExpect(jsonPath("$.toWarehouse.name").value("Cairo Central Hub"))
                .andExpect(jsonPath("$.notes").value("Received at Cairo"));

        // Verify Shipment.currentWarehouse in DB
        Shipment afterFirst = shipmentRepository.findById(shipment.getId()).orElseThrow();
        assertNotNull(afterFirst.getCurrentWarehouse());
        assertEquals(warehouseCairo.getId(), afterFirst.getCurrentWarehouse().getId());

        // 2. Subsequent movement from Cairo to Alexandria Port Hub
        MoveShipmentToWarehouseRequest req2 = new MoveShipmentToWarehouseRequest(warehouseAlex.getId(), "Transferred to Alexandria");

        mockMvc.perform(patch("/api/shipments/{shipmentId}/warehouse", shipment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromWarehouse.id").value(warehouseCairo.getId()))
                .andExpect(jsonPath("$.fromWarehouse.name").value("Cairo Central Hub"))
                .andExpect(jsonPath("$.toWarehouse.id").value(warehouseAlex.getId()))
                .andExpect(jsonPath("$.toWarehouse.name").value("Alexandria Port Hub"))
                .andExpect(jsonPath("$.notes").value("Transferred to Alexandria"));

        // Verify Shipment.currentWarehouse in DB
        Shipment afterSecond = shipmentRepository.findById(shipment.getId()).orElseThrow();
        assertEquals(warehouseAlex.getId(), afterSecond.getCurrentWarehouse().getId());

        // 3. Verify Tracking events appended
        List<ShipmentTracking> trackings = shipmentTrackingRepository.findByShipmentIdOrderByCreatedAtAsc(shipment.getId());
        assertEquals(3, trackings.size());
        assertEquals("Shipment created", trackings.get(0).getDescription());
        assertEquals("Shipment moved to warehouse: Cairo Central Hub", trackings.get(1).getDescription());
        assertEquals("Shipment moved to warehouse: Alexandria Port Hub", trackings.get(2).getDescription());

        // 4. Verify GET /api/shipments/{shipmentId} exposes currentWarehouse summary
        mockMvc.perform(get("/api/shipments/{id}", shipment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentWarehouse.id").value(warehouseAlex.getId()))
                .andExpect(jsonPath("$.currentWarehouse.name").value("Alexandria Port Hub"));

        // 5. Verify GET /api/shipments/{shipmentId}/warehouse-movements
        mockMvc.perform(get("/api/shipments/{shipmentId}/warehouse-movements", shipment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].fromWarehouse", nullValue()))
                .andExpect(jsonPath("$[0].toWarehouse.id").value(warehouseCairo.getId()))
                .andExpect(jsonPath("$[1].fromWarehouse.id").value(warehouseCairo.getId()))
                .andExpect(jsonPath("$[1].toWarehouse.id").value(warehouseAlex.getId()));
    }

    @Test
    @DisplayName("DISPATCHER can move shipment to warehouse")
    @WithMockUser(username = "disp@logistics.com", roles = {"DISPATCHER"})
    void moveShipment_asDispatcher_success() throws Exception {
        MoveShipmentToWarehouseRequest req = new MoveShipmentToWarehouseRequest(warehouseCairo.getId(), "Arrived");

        mockMvc.perform(patch("/api/shipments/{shipmentId}/warehouse", shipment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toWarehouse.name").value("Cairo Central Hub"));
    }

    @Test
    @DisplayName("CUSTOMER role is forbidden from moving shipment (403)")
    @WithMockUser(username = "wm_cust1@logistics.com", roles = {"CUSTOMER"})
    void moveShipment_asCustomer_forbidden() throws Exception {
        MoveShipmentToWarehouseRequest req = new MoveShipmentToWarehouseRequest(warehouseCairo.getId(), "notes");

        mockMvc.perform(patch("/api/shipments/{shipmentId}/warehouse", shipment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DRIVER role is forbidden from moving shipment (403)")
    @WithMockUser(username = "driver@logistics.com", roles = {"DRIVER"})
    void moveShipment_asDriver_forbidden() throws Exception {
        MoveShipmentToWarehouseRequest req = new MoveShipmentToWarehouseRequest(warehouseCairo.getId(), "notes");

        mockMvc.perform(patch("/api/shipments/{shipmentId}/warehouse", shipment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request to move shipment returns 401")
    void moveShipment_unauthenticated_returns401() throws Exception {
        MoveShipmentToWarehouseRequest req = new MoveShipmentToWarehouseRequest(warehouseCairo.getId(), "notes");

        mockMvc.perform(patch("/api/shipments/{shipmentId}/warehouse", shipment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Moving to non-active warehouse returns 400 Bad Request")
    @WithMockUser(username = "admin@logistics.com", roles = {"ADMIN"})
    void moveShipment_warehouseNotActive_returns400() throws Exception {
        MoveShipmentToWarehouseRequest req = new MoveShipmentToWarehouseRequest(warehouseInactive.getId(), "notes");

        mockMvc.perform(patch("/api/shipments/{shipmentId}/warehouse", shipment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Cannot move shipment to warehouse with status: MAINTENANCE")));
    }

    @Test
    @DisplayName("Moving to same current warehouse returns 400 Bad Request")
    @WithMockUser(username = "admin@logistics.com", roles = {"ADMIN"})
    void moveShipment_sameWarehouse_returns400() throws Exception {
        // Move to Cairo first
        MoveShipmentToWarehouseRequest req = new MoveShipmentToWarehouseRequest(warehouseCairo.getId(), "First");
        mockMvc.perform(patch("/api/shipments/{shipmentId}/warehouse", shipment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Try moving to Cairo again
        mockMvc.perform(patch("/api/shipments/{shipmentId}/warehouse", shipment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Shipment is already currently at warehouse: Cairo Central Hub")));
    }

    @Test
    @DisplayName("Owner customer can view warehouse movements (200)")
    @WithMockUser(username = "wm_cust1@logistics.com", roles = {"CUSTOMER"})
    void getMovements_asOwnerCustomer_returns200() throws Exception {
        mockMvc.perform(get("/api/shipments/{shipmentId}/warehouse-movements", shipment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Other customer is forbidden from viewing warehouse movements (403)")
    @WithMockUser(username = "wm_cust2@logistics.com", roles = {"CUSTOMER"})
    void getMovements_asOtherCustomer_returns403() throws Exception {
        mockMvc.perform(get("/api/shipments/{shipmentId}/warehouse-movements", shipment.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DRIVER is forbidden from viewing warehouse movements (403)")
    @WithMockUser(username = "driver@logistics.com", roles = {"DRIVER"})
    void getMovements_asDriver_returns403() throws Exception {
        mockMvc.perform(get("/api/shipments/{shipmentId}/warehouse-movements", shipment.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request to get warehouse movements returns 401")
    void getMovements_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/shipments/{shipmentId}/warehouse-movements", shipment.getId()))
                .andExpect(status().isUnauthorized());
    }
}
