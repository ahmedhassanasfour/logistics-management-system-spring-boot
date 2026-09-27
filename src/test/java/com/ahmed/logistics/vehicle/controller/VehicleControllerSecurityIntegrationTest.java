package com.ahmed.logistics.vehicle.controller;

import com.ahmed.logistics.user.entity.Role;
import com.ahmed.logistics.user.entity.User;
import com.ahmed.logistics.user.repository.UserRepository;
import com.ahmed.logistics.vehicle.dto.CreateVehicleRequest;
import com.ahmed.logistics.vehicle.dto.UpdateVehicleRequest;
import com.ahmed.logistics.vehicle.entity.Vehicle;
import com.ahmed.logistics.vehicle.entity.VehicleStatus;
import com.ahmed.logistics.vehicle.entity.VehicleType;
import com.ahmed.logistics.vehicle.repository.VehicleRepository;
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
class VehicleControllerSecurityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MockMvc mockMvc;

    private Vehicle sampleVehicle;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Create Admin user
        User admin = User.builder()
                .email("admin_vehicle_test@logistics.com")
                .password("$2a$10$hashedPass")
                .firstName("Admin")
                .lastName("User")
                .role(Role.ADMIN)
                .enabled(true)
                .build();
        userRepository.saveAndFlush(admin);

        // Create Driver user (Non-admin)
        User driver = User.builder()
                .email("driver_vehicle_test@logistics.com")
                .password("$2a$10$hashedPass")
                .firstName("Driver")
                .lastName("User")
                .role(Role.DRIVER)
                .enabled(true)
                .build();
        userRepository.saveAndFlush(driver);

        // Create Customer user (Non-admin)
        User customer = User.builder()
                .email("customer_vehicle_test@logistics.com")
                .password("$2a$10$hashedPass")
                .firstName("Customer")
                .lastName("User")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();
        userRepository.saveAndFlush(customer);

        // Create existing vehicle
        sampleVehicle = Vehicle.builder()
                .plateNumber("EXISTING-100")
                .type(VehicleType.VAN)
                .status(VehicleStatus.AVAILABLE)
                .brand("Renault")
                .model("Master")
                .manufacturingYear(2022)
                .maxWeightKg(3500.0)
                .build();
        sampleVehicle = vehicleRepository.saveAndFlush(sampleVehicle);
    }

    @Test
    void unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/vehicles/" + sampleVehicle.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(username = "admin_vehicle_test@logistics.com", roles = "ADMIN")
    void admin_canCreateVehicle() throws Exception {
        CreateVehicleRequest request = new CreateVehicleRequest(
                "NEW-PLATE-001",
                VehicleType.TRUCK,
                "Volvo",
                "FH",
                2023,
                18000.0
        );

        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plateNumber").value("NEW-PLATE-001"))
                .andExpect(jsonPath("$.type").value("TRUCK"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.brand").value("Volvo"))
                .andExpect(jsonPath("$.model").value("FH"))
                .andExpect(jsonPath("$.manufacturingYear").value(2023))
                .andExpect(jsonPath("$.maxWeightKg").value(18000.0));
    }

    @Test
    @WithMockUser(username = "admin_vehicle_test@logistics.com", roles = "ADMIN")
    void admin_canReadVehicleById() throws Exception {
        mockMvc.perform(get("/api/vehicles/" + sampleVehicle.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sampleVehicle.getId()))
                .andExpect(jsonPath("$.plateNumber").value("EXISTING-100"))
                .andExpect(jsonPath("$.brand").value("Renault"));
    }

    @Test
    @WithMockUser(username = "admin_vehicle_test@logistics.com", roles = "ADMIN")
    void admin_canReadVehicleByPlateNumber() throws Exception {
        mockMvc.perform(get("/api/vehicles/plate/" + sampleVehicle.getPlateNumber()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sampleVehicle.getId()))
                .andExpect(jsonPath("$.plateNumber").value("EXISTING-100"));
    }

    @Test
    @WithMockUser(username = "admin_vehicle_test@logistics.com", roles = "ADMIN")
    void admin_canUpdateVehicle() throws Exception {
        UpdateVehicleRequest updateRequest = new UpdateVehicleRequest(
                "UPDATED-PLATE-100",
                VehicleType.VAN,
                "Renault",
                "Master Pro",
                2023,
                3800.0
        );

        mockMvc.perform(put("/api/vehicles/" + sampleVehicle.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sampleVehicle.getId()))
                .andExpect(jsonPath("$.plateNumber").value("UPDATED-PLATE-100"))
                .andExpect(jsonPath("$.model").value("Master Pro"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    @WithMockUser(username = "admin_vehicle_test@logistics.com", roles = "ADMIN")
    void admin_canDeleteVehicle() throws Exception {
        mockMvc.perform(delete("/api/vehicles/" + sampleVehicle.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "driver_vehicle_test@logistics.com", roles = "DRIVER")
    void nonAdmin_cannotCreateVehicle() throws Exception {
        CreateVehicleRequest request = new CreateVehicleRequest(
                "HACK-001",
                VehicleType.CAR,
                "Toyota",
                "Corolla",
                2022,
                1500.0
        );

        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @WithMockUser(username = "driver_vehicle_test@logistics.com", roles = "DRIVER")
    void nonAdmin_cannotUpdateVehicle() throws Exception {
        UpdateVehicleRequest request = new UpdateVehicleRequest(
                "HACK-002",
                VehicleType.CAR,
                "Toyota",
                "Corolla",
                2022,
                1500.0
        );

        mockMvc.perform(put("/api/vehicles/" + sampleVehicle.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @WithMockUser(username = "driver_vehicle_test@logistics.com", roles = "DRIVER")
    void nonAdmin_cannotDeleteVehicle() throws Exception {
        mockMvc.perform(delete("/api/vehicles/" + sampleVehicle.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @WithMockUser(username = "admin_vehicle_test@logistics.com", roles = "ADMIN")
    void createVehicle_validationRejectsInvalidRequest() throws Exception {
        CreateVehicleRequest invalidRequest = new CreateVehicleRequest(
                "",       // blank plate
                null,     // null type
                "",       // blank brand
                "",       // blank model
                null,     // null year
                -50.0     // negative weight
        );

        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.plateNumber").isNotEmpty())
                .andExpect(jsonPath("$.type").isNotEmpty())
                .andExpect(jsonPath("$.brand").isNotEmpty())
                .andExpect(jsonPath("$.model").isNotEmpty())
                .andExpect(jsonPath("$.manufacturingYear").isNotEmpty())
                .andExpect(jsonPath("$.maxWeightKg").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "admin_vehicle_test@logistics.com", roles = "ADMIN")
    void createVehicle_duplicatePlateNumberReturns400() throws Exception {
        CreateVehicleRequest duplicateRequest = new CreateVehicleRequest(
                "EXISTING-100", // already exists
                VehicleType.VAN,
                "Fiat",
                "Ducato",
                2023,
                3000.0
        );

        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already exists")));
    }
}
