package com.ahmed.logistics.driver.controller;

import com.ahmed.logistics.driver.dto.CreateDriverRequest;
import com.ahmed.logistics.driver.dto.UpdateDriverRequest;
import com.ahmed.logistics.driver.entity.Driver;
import com.ahmed.logistics.driver.entity.DriverStatus;
import com.ahmed.logistics.driver.repository.DriverRepository;
import com.ahmed.logistics.user.entity.Role;
import com.ahmed.logistics.user.entity.User;
import com.ahmed.logistics.user.repository.UserRepository;
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

import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class DriverControllerSecurityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DriverRepository driverRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MockMvc mockMvc;

    private Driver driver1;
    private Driver driver2;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Create User 1 (Driver)
        User user1 = User.builder()
                .email("driver1@logistics.com")
                .password("$2a$10$hashedPass")
                .firstName("Lewis")
                .lastName("Hamilton")
                .role(Role.DRIVER)
                .enabled(true)
                .build();
        user1 = userRepository.saveAndFlush(user1);

        driver1 = Driver.builder()
                .user(user1)
                .phone("+1111111111")
                .licenseNumber("LIC-DRV-001")
                .licenseExpiryDate(LocalDate.now().plusYears(3))
                .status(DriverStatus.OFFLINE)
                .build();
        driver1 = driverRepository.saveAndFlush(driver1);

        // Create User 2 (Driver)
        User user2 = User.builder()
                .email("driver2@logistics.com")
                .password("$2a$10$hashedPass")
                .firstName("Max")
                .lastName("Verstappen")
                .role(Role.DRIVER)
                .enabled(true)
                .build();
        user2 = userRepository.saveAndFlush(user2);

        driver2 = Driver.builder()
                .user(user2)
                .phone("+2222222222")
                .licenseNumber("LIC-DRV-002")
                .licenseExpiryDate(LocalDate.now().plusYears(2))
                .status(DriverStatus.OFFLINE)
                .build();
        driver2 = driverRepository.saveAndFlush(driver2);

        // Create User 3 (Admin)
        User admin = User.builder()
                .email("admin@logistics.com")
                .password("$2a$10$hashedPass")
                .firstName("Super")
                .lastName("Admin")
                .role(Role.ADMIN)
                .enabled(true)
                .build();
        userRepository.saveAndFlush(admin);
    }

    @Test
    void unauthenticatedUser_cannotAccessDriverEndpoints() throws Exception {
        mockMvc.perform(get("/api/drivers/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(username = "driver1@logistics.com", roles = "DRIVER")
    void driver_canAccessOwnProfileThroughMe() throws Exception {
        mockMvc.perform(get("/api/drivers/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(driver1.getId()))
                .andExpect(jsonPath("$.email").value("driver1@logistics.com"))
                .andExpect(jsonPath("$.phone").value("+1111111111"))
                .andExpect(jsonPath("$.licenseNumber").value("LIC-DRV-001"))
                .andExpect(jsonPath("$.status").value("OFFLINE"));
    }

    @Test
    @WithMockUser(username = "driver1@logistics.com", roles = "DRIVER")
    void driver_canAccessOwnProfileById() throws Exception {
        mockMvc.perform(get("/api/drivers/" + driver1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(driver1.getId()))
                .andExpect(jsonPath("$.email").value("driver1@logistics.com"));
    }

    @Test
    @WithMockUser(username = "driver1@logistics.com", roles = "DRIVER")
    void driver_cannotAccessAnotherDriverProfile() throws Exception {
        mockMvc.perform(get("/api/drivers/" + driver2.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @WithMockUser(username = "admin@logistics.com", roles = "ADMIN")
    void admin_canAccessAnyDriverProfile() throws Exception {
        mockMvc.perform(get("/api/drivers/" + driver2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(driver2.getId()))
                .andExpect(jsonPath("$.email").value("driver2@logistics.com"));
    }

    @Test
    @WithMockUser(username = "driver1@logistics.com", roles = "DRIVER")
    void driver_canUpdateOwnProfile() throws Exception {
        UpdateDriverRequest updateRequest = new UpdateDriverRequest(
                "+1999888777",
                "LIC-DRV-001-UPDATED",
                LocalDate.now().plusYears(4)
        );

        mockMvc.perform(put("/api/drivers/" + driver1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(driver1.getId()))
                .andExpect(jsonPath("$.phone").value("+1999888777"))
                .andExpect(jsonPath("$.licenseNumber").value("LIC-DRV-001-UPDATED"))
                .andExpect(jsonPath("$.status").value("OFFLINE"));
    }

    @Test
    @WithMockUser(username = "driver1@logistics.com", roles = "DRIVER")
    void driver_cannotUpdateAnotherDriverProfile() throws Exception {
        UpdateDriverRequest updateRequest = new UpdateDriverRequest(
                "+1999888777",
                "LIC-DRV-HACK",
                LocalDate.now().plusYears(4)
        );

        mockMvc.perform(put("/api/drivers/" + driver2.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @WithMockUser(username = "admin@logistics.com", roles = "ADMIN")
    void admin_canUpdateAnyDriverProfile() throws Exception {
        UpdateDriverRequest updateRequest = new UpdateDriverRequest(
                "+1777666555",
                "LIC-DRV-002-ADMIN-MOD",
                LocalDate.now().plusYears(5)
        );

        mockMvc.perform(put("/api/drivers/" + driver2.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(driver2.getId()))
                .andExpect(jsonPath("$.phone").value("+1777666555"))
                .andExpect(jsonPath("$.licenseNumber").value("LIC-DRV-002-ADMIN-MOD"));
    }

    @Test
    @WithMockUser(username = "driver1@logistics.com", roles = "DRIVER")
    void driver_canDeleteOwnProfile() throws Exception {
        mockMvc.perform(delete("/api/drivers/" + driver1.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "driver1@logistics.com", roles = "DRIVER")
    void driver_cannotDeleteAnotherDriverProfile() throws Exception {
        mockMvc.perform(delete("/api/drivers/" + driver2.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @WithMockUser(username = "admin@logistics.com", roles = "ADMIN")
    void admin_canDeleteAnyDriverProfile() throws Exception {
        mockMvc.perform(delete("/api/drivers/" + driver2.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "driver1@logistics.com", roles = "DRIVER")
    void createDriver_validationRejectsBlankRequiredFieldsAndPastDate() throws Exception {
        CreateDriverRequest invalidRequest = new CreateDriverRequest(
                null,
                "",
                "",
                LocalDate.now().minusDays(1) // past date
        );

        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.phone").isNotEmpty())
                .andExpect(jsonPath("$.licenseNumber").isNotEmpty())
                .andExpect(jsonPath("$.licenseExpiryDate").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "admin@logistics.com", roles = "ADMIN")
    void createDriver_createsDriverProfileSuccessfully() throws Exception {
        // Create new user for this test
        User newUser = User.builder()
                .email("newdriver@logistics.com")
                .password("$2a$10$hashedPass")
                .firstName("Charles")
                .lastName("Leclerc")
                .role(Role.DRIVER)
                .enabled(true)
                .build();
        newUser = userRepository.saveAndFlush(newUser);

        CreateDriverRequest request = new CreateDriverRequest(
                newUser.getId(),
                "+1555444333",
                "LIC-NEW-001",
                LocalDate.now().plusYears(3)
        );

        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(newUser.getId()))
                .andExpect(jsonPath("$.email").value("newdriver@logistics.com"))
                .andExpect(jsonPath("$.phone").value("+1555444333"))
                .andExpect(jsonPath("$.licenseNumber").value("LIC-NEW-001"))
                .andExpect(jsonPath("$.status").value("OFFLINE"));
    }
}
