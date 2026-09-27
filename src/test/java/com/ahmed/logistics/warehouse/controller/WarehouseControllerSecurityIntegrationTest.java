package com.ahmed.logistics.warehouse.controller;

import com.ahmed.logistics.user.entity.Role;
import com.ahmed.logistics.user.entity.User;
import com.ahmed.logistics.user.repository.UserRepository;
import com.ahmed.logistics.warehouse.dto.CreateWarehouseRequest;
import com.ahmed.logistics.warehouse.dto.UpdateWarehouseRequest;
import com.ahmed.logistics.warehouse.entity.Warehouse;
import com.ahmed.logistics.warehouse.entity.WarehouseStatus;
import com.ahmed.logistics.warehouse.repository.WarehouseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class WarehouseControllerSecurityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MockMvc mockMvc;

    private Warehouse sampleWarehouse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Create Admin user
        User admin = User.builder()
                .email("admin_wh_test@logistics.com")
                .password("$2a$10$hashedPass")
                .firstName("Admin")
                .lastName("Warehouse")
                .role(Role.ADMIN)
                .enabled(true)
                .build();
        userRepository.saveAndFlush(admin);

        // Create Driver user (Non-admin)
        User driver = User.builder()
                .email("driver_wh_test@logistics.com")
                .password("$2a$10$hashedPass")
                .firstName("Driver")
                .lastName("User")
                .role(Role.DRIVER)
                .enabled(true)
                .build();
        userRepository.saveAndFlush(driver);

        // Create Customer user (Non-admin)
        User customer = User.builder()
                .email("customer_wh_test@logistics.com")
                .password("$2a$10$hashedPass")
                .firstName("Customer")
                .lastName("User")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();
        userRepository.saveAndFlush(customer);

        // Create sample warehouse
        sampleWarehouse = Warehouse.builder()
                .name("Phoenix Logistics Center")
                .address("700 Desert Ridge")
                .city("Phoenix")
                .postalCode("85001")
                .phone("+16025550199")
                .email("phoenix-wh@logistics.com")
                .status(WarehouseStatus.ACTIVE)
                .build();
        sampleWarehouse = warehouseRepository.saveAndFlush(sampleWarehouse);
    }

    @Test
    void unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/warehouses/" + sampleWarehouse.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(username = "admin_wh_test@logistics.com", roles = "ADMIN")
    void admin_canCreateWarehouse() throws Exception {
        CreateWarehouseRequest request = new CreateWarehouseRequest(
                "Salt Lake Depot",
                "300 Mountain View",
                "Salt Lake City",
                "84101",
                "+18015550133",
                "slc-wh@logistics.com"
        );

        mockMvc.perform(post("/api/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Salt Lake Depot"))
                .andExpect(jsonPath("$.address").value("300 Mountain View"))
                .andExpect(jsonPath("$.city").value("Salt Lake City"))
                .andExpect(jsonPath("$.postalCode").value("84101"))
                .andExpect(jsonPath("$.phone").value("+18015550133"))
                .andExpect(jsonPath("$.email").value("slc-wh@logistics.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(username = "admin_wh_test@logistics.com", roles = "ADMIN")
    void admin_canReadWarehouseById() throws Exception {
        mockMvc.perform(get("/api/warehouses/" + sampleWarehouse.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sampleWarehouse.getId()))
                .andExpect(jsonPath("$.name").value("Phoenix Logistics Center"))
                .andExpect(jsonPath("$.city").value("Phoenix"));
    }

    @Test
    @WithMockUser(username = "admin_wh_test@logistics.com", roles = "ADMIN")
    void admin_canReadWarehouseByName() throws Exception {
        mockMvc.perform(get("/api/warehouses/name/" + sampleWarehouse.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sampleWarehouse.getId()))
                .andExpect(jsonPath("$.name").value("Phoenix Logistics Center"));
    }

    @Test
    @WithMockUser(username = "admin_wh_test@logistics.com", roles = "ADMIN")
    void admin_canUpdateWarehouse() throws Exception {
        UpdateWarehouseRequest updateRequest = new UpdateWarehouseRequest(
                "Phoenix Super Hub",
                "700 Desert Ridge Suite 100",
                "Phoenix",
                "85002",
                "+16025550299",
                "phoenix-hub@logistics.com"
        );

        mockMvc.perform(put("/api/warehouses/" + sampleWarehouse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sampleWarehouse.getId()))
                .andExpect(jsonPath("$.name").value("Phoenix Super Hub"))
                .andExpect(jsonPath("$.postalCode").value("85002"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(username = "admin_wh_test@logistics.com", roles = "ADMIN")
    void admin_canDeleteWarehouse() throws Exception {
        mockMvc.perform(delete("/api/warehouses/" + sampleWarehouse.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "driver_wh_test@logistics.com", roles = "DRIVER")
    void nonAdmin_cannotCreateWarehouse() throws Exception {
        CreateWarehouseRequest request = new CreateWarehouseRequest(
                "Unauthorized Warehouse",
                "123 Fake St",
                "Nowhere",
                "00000",
                "+10000000000",
                "unauth@logistics.com"
        );

        mockMvc.perform(post("/api/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @WithMockUser(username = "driver_wh_test@logistics.com", roles = "DRIVER")
    void nonAdmin_cannotUpdateWarehouse() throws Exception {
        UpdateWarehouseRequest request = new UpdateWarehouseRequest(
                "Hacked Warehouse",
                "123 Fake St",
                "Nowhere",
                "00000",
                "+10000000000",
                "hacked@logistics.com"
        );

        mockMvc.perform(put("/api/warehouses/" + sampleWarehouse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @WithMockUser(username = "driver_wh_test@logistics.com", roles = "DRIVER")
    void nonAdmin_cannotDeleteWarehouse() throws Exception {
        mockMvc.perform(delete("/api/warehouses/" + sampleWarehouse.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @WithMockUser(username = "admin_wh_test@logistics.com", roles = "ADMIN")
    void createWarehouse_validationRejectsInvalidRequest() throws Exception {
        CreateWarehouseRequest invalidRequest = new CreateWarehouseRequest(
                "",               // blank name
                "",               // blank address
                "",               // blank city
                "",               // blank postal code
                "",               // blank phone
                "not-an-email"    // invalid email
        );

        mockMvc.perform(post("/api/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").isNotEmpty())
                .andExpect(jsonPath("$.address").isNotEmpty())
                .andExpect(jsonPath("$.city").isNotEmpty())
                .andExpect(jsonPath("$.postalCode").isNotEmpty())
                .andExpect(jsonPath("$.phone").isNotEmpty())
                .andExpect(jsonPath("$.email").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "admin_wh_test@logistics.com", roles = "ADMIN")
    void createWarehouse_duplicateNameReturns400() throws Exception {
        CreateWarehouseRequest duplicateRequest = new CreateWarehouseRequest(
                "Phoenix Logistics Center", // duplicate name
                "123 Some Other Way",
                "Phoenix",
                "85002",
                "+16025559999",
                "dup@logistics.com"
        );

        mockMvc.perform(post("/api/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already exists")));
    }
}
