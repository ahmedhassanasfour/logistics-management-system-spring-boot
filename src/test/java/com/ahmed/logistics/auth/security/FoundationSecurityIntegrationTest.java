package com.ahmed.logistics.auth.security;

import com.ahmed.logistics.auth.jwt.JwtService;
import com.ahmed.logistics.customer.entity.Customer;
import com.ahmed.logistics.customer.repository.CustomerRepository;
import com.ahmed.logistics.driver.entity.Driver;
import com.ahmed.logistics.driver.entity.DriverStatus;
import com.ahmed.logistics.driver.repository.DriverRepository;
import com.ahmed.logistics.user.entity.Role;
import com.ahmed.logistics.user.entity.User;
import com.ahmed.logistics.user.repository.UserRepository;
import com.ahmed.logistics.vehicle.dto.CreateVehicleRequest;
import com.ahmed.logistics.vehicle.entity.Vehicle;
import com.ahmed.logistics.vehicle.entity.VehicleStatus;
import com.ahmed.logistics.vehicle.entity.VehicleType;
import com.ahmed.logistics.vehicle.repository.VehicleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
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
class FoundationSecurityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MockMvc mockMvc;

    private User adminUser;
    private User dispatcherUser;
    private User customerUser1;
    private User customerUser2;
    private User driverUser1;
    private User driverUser2;

    private Customer customer1;
    private Customer customer2;
    private Driver driver1;
    private Driver driver2;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // 1. Admin
        adminUser = userRepository.saveAndFlush(User.builder()
                .email("admin_sec@logistics.com")
                .password(passwordEncoder.encode("Password123!"))
                .firstName("Admin")
                .lastName("Sec")
                .role(Role.ADMIN)
                .enabled(true)
                .build());

        // 2. Dispatcher
        dispatcherUser = userRepository.saveAndFlush(User.builder()
                .email("dispatcher_sec@logistics.com")
                .password(passwordEncoder.encode("Password123!"))
                .firstName("Dispatcher")
                .lastName("Sec")
                .role(Role.DISPATCHER)
                .enabled(true)
                .build());

        // 3. Customer 1 & 2
        customerUser1 = userRepository.saveAndFlush(User.builder()
                .email("cust1_sec@logistics.com")
                .password(passwordEncoder.encode("Password123!"))
                .firstName("Customer")
                .lastName("One")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build());

        customer1 = customerRepository.saveAndFlush(Customer.builder()
                .user(customerUser1)
                .phone("+1555111111")
                .address("100 Sec St")
                .city("Dallas")
                .postalCode("75001")
                .build());

        customerUser2 = userRepository.saveAndFlush(User.builder()
                .email("cust2_sec@logistics.com")
                .password(passwordEncoder.encode("Password123!"))
                .firstName("Customer")
                .lastName("Two")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build());

        customer2 = customerRepository.saveAndFlush(Customer.builder()
                .user(customerUser2)
                .phone("+1555222222")
                .address("200 Sec St")
                .city("Houston")
                .postalCode("77001")
                .build());

        // 4. Driver 1 & 2
        driverUser1 = userRepository.saveAndFlush(User.builder()
                .email("drv1_sec@logistics.com")
                .password(passwordEncoder.encode("Password123!"))
                .firstName("Driver")
                .lastName("One")
                .role(Role.DRIVER)
                .enabled(true)
                .build());

        driver1 = driverRepository.saveAndFlush(Driver.builder()
                .user(driverUser1)
                .phone("+1555333333")
                .licenseNumber("LIC-SEC-001")
                .licenseExpiryDate(LocalDate.now().plusYears(2))
                .status(DriverStatus.OFFLINE)
                .build());

        driverUser2 = userRepository.saveAndFlush(User.builder()
                .email("drv2_sec@logistics.com")
                .password(passwordEncoder.encode("Password123!"))
                .firstName("Driver")
                .lastName("Two")
                .role(Role.DRIVER)
                .enabled(true)
                .build());

        driver2 = driverRepository.saveAndFlush(Driver.builder()
                .user(driverUser2)
                .phone("+1555444444")
                .licenseNumber("LIC-SEC-002")
                .licenseExpiryDate(LocalDate.now().plusYears(2))
                .status(DriverStatus.OFFLINE)
                .build());

        // 5. Vehicle
        vehicle = vehicleRepository.saveAndFlush(Vehicle.builder()
                .plateNumber("SEC-INIT-01")
                .type(VehicleType.VAN)
                .brand("Mercedes")
                .model("Sprinter")
                .manufacturingYear(2022)
                .maxWeightKg(3500.0)
                .status(VehicleStatus.AVAILABLE)
                .build());
    }

    // ==========================================
    // 1. Authentication Integration Tests
    // ==========================================

    @Test
    @DisplayName("Unauthenticated protected request returns 401 Unauthorized")
    void unauthenticatedProtectedRequest_returns401() throws Exception {
        mockMvc.perform(get("/api/vehicles/" + vehicle.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Valid JWT access token authenticates protected request")
    void validJwtAccessToken_authenticatesProtectedRequest() throws Exception {
        CustomUserDetails userDetails = new CustomUserDetails(customerUser1);
        String accessToken = jwtService.generateAccessToken(userDetails);

        mockMvc.perform(get("/api/vehicles/" + vehicle.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vehicle.getId()))
                .andExpect(jsonPath("$.plateNumber").value("SEC-INIT-01"));
    }

    @Test
    @DisplayName("Invalid JWT token returns 401 Unauthorized")
    void invalidJwtToken_returns401() throws Exception {
        mockMvc.perform(get("/api/vehicles/" + vehicle.getId())
                        .header("Authorization", "Bearer invalid.jwt.token.here"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Refresh token used as access token is rejected with 401 Unauthorized")
    void refreshTokenUsedAsAccessToken_isRejected() throws Exception {
        CustomUserDetails userDetails = new CustomUserDetails(adminUser);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        mockMvc.perform(get("/api/vehicles/" + vehicle.getId())
                        .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    // ==========================================
    // 2. Authorization & Role Matrix Tests
    // ==========================================

    @Test
    @DisplayName("ADMIN administrative operation is allowed")
    @WithMockUser(username = "admin_sec@logistics.com", roles = "ADMIN")
    void admin_administrativeOperation_isAllowed() throws Exception {
        CreateVehicleRequest request = new CreateVehicleRequest(
                "SEC-PLT-100",
                VehicleType.TRUCK,
                "Volvo",
                "FH16",
                2023,
                18000.0
        );

        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plateNumber").value("SEC-PLT-100"));
    }

    @Test
    @DisplayName("CUSTOMER administrative operation is rejected with 403 Forbidden")
    @WithMockUser(username = "cust1_sec@logistics.com", roles = "CUSTOMER")
    void customer_administrativeOperation_isForbidden() throws Exception {
        CreateVehicleRequest request = new CreateVehicleRequest(
                "SEC-PLT-101",
                VehicleType.TRUCK,
                "Volvo",
                "FH16",
                2023,
                18000.0
        );

        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("DRIVER administrative operation is rejected with 403 Forbidden")
    @WithMockUser(username = "drv1_sec@logistics.com", roles = "DRIVER")
    void driver_administrativeOperation_isForbidden() throws Exception {
        CreateVehicleRequest request = new CreateVehicleRequest(
                "SEC-PLT-102",
                VehicleType.TRUCK,
                "Volvo",
                "FH16",
                2023,
                18000.0
        );

        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("DISPATCHER administrative operation is rejected with 403 Forbidden")
    @WithMockUser(username = "dispatcher_sec@logistics.com", roles = "DISPATCHER")
    void dispatcher_administrativeOperation_isForbidden() throws Exception {
        CreateVehicleRequest request = new CreateVehicleRequest(
                "SEC-PLT-103",
                VehicleType.TRUCK,
                "Volvo",
                "FH16",
                2023,
                18000.0
        );

        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("DISPATCHER can read customer and driver by ID")
    @WithMockUser(username = "dispatcher_sec@logistics.com", roles = "DISPATCHER")
    void dispatcher_canReadCustomerAndDriver() throws Exception {
        // Read Customer
        mockMvc.perform(get("/api/customers/" + customer1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customer1.getId()));

        // Read Driver
        mockMvc.perform(get("/api/drivers/" + driver1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(driver1.getId()));
    }

    @Test
    @DisplayName("DISPATCHER cannot update customer or driver")
    @WithMockUser(username = "dispatcher_sec@logistics.com", roles = "DISPATCHER")
    void dispatcher_cannotUpdateCustomerOrDriver() throws Exception {
        mockMvc.perform(delete("/api/customers/" + customer1.getId()))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/drivers/" + driver1.getId()))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // 3. Ownership Integration Tests
    // ==========================================

    @Test
    @DisplayName("CUSTOMER A cannot access CUSTOMER B profile")
    @WithMockUser(username = "cust1_sec@logistics.com", roles = "CUSTOMER")
    void customerA_cannotAccessCustomerBProfile() throws Exception {
        mockMvc.perform(get("/api/customers/" + customer2.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("DRIVER A cannot access DRIVER B private profile")
    @WithMockUser(username = "drv1_sec@logistics.com", roles = "DRIVER")
    void driverA_cannotAccessDriverBProfile() throws Exception {
        mockMvc.perform(get("/api/drivers/" + driver2.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("ADMIN can access any customer profile")
    @WithMockUser(username = "admin_sec@logistics.com", roles = "ADMIN")
    void admin_canAccessAnyCustomerProfile() throws Exception {
        mockMvc.perform(get("/api/customers/" + customer1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customer1.getId()));
    }

    @Test
    @DisplayName("CUSTOMER can access own profile")
    @WithMockUser(username = "cust1_sec@logistics.com", roles = "CUSTOMER")
    void customer_canAccessOwnProfile() throws Exception {
        mockMvc.perform(get("/api/customers/" + customer1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customer1.getId()));
    }

    @Test
    @DisplayName("ADMIN can access any driver profile")
    @WithMockUser(username = "admin_sec@logistics.com", roles = "ADMIN")
    void admin_canAccessAnyDriverProfile() throws Exception {
        mockMvc.perform(get("/api/drivers/" + driver1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(driver1.getId()));
    }

    @Test
    @DisplayName("DRIVER can access own profile")
    @WithMockUser(username = "drv1_sec@logistics.com", roles = "DRIVER")
    void driver_canAccessOwnProfile() throws Exception {
        mockMvc.perform(get("/api/drivers/" + driver1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(driver1.getId()));
    }
}
