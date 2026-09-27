package com.ahmed.logistics.customer.controller;

import com.ahmed.logistics.customer.dto.CreateCustomerRequest;
import com.ahmed.logistics.customer.entity.Customer;
import com.ahmed.logistics.customer.repository.CustomerRepository;
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

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class CustomerControllerSecurityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MockMvc mockMvc;

    private Customer customer1;
    private Customer customer2;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Create User 1 (Customer)
        User user1 = User.builder()
                .email("customer1@logistics.com")
                .password("$2a$10$hashedPass")
                .firstName("John")
                .lastName("Doe")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();
        user1 = userRepository.saveAndFlush(user1);

        customer1 = Customer.builder()
                .user(user1)
                .phone("+1111111111")
                .address("100 First St")
                .city("Dallas")
                .postalCode("75001")
                .build();
        customer1 = customerRepository.saveAndFlush(customer1);

        // Create User 2 (Customer)
        User user2 = User.builder()
                .email("customer2@logistics.com")
                .password("$2a$10$hashedPass")
                .firstName("Jane")
                .lastName("Smith")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();
        user2 = userRepository.saveAndFlush(user2);

        customer2 = Customer.builder()
                .user(user2)
                .phone("+2222222222")
                .address("200 Second St")
                .city("Houston")
                .postalCode("77001")
                .build();
        customer2 = customerRepository.saveAndFlush(customer2);

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
    @WithMockUser(username = "customer1@logistics.com", roles = "CUSTOMER")
    void customer_canAccessOwnProfileThroughMe() throws Exception {
        mockMvc.perform(get("/api/customers/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customer1.getId()))
                .andExpect(jsonPath("$.email").value("customer1@logistics.com"))
                .andExpect(jsonPath("$.phone").value("+1111111111"))
                .andExpect(jsonPath("$.city").value("Dallas"));
    }

    @Test
    @WithMockUser(username = "customer1@logistics.com", roles = "CUSTOMER")
    void customer_canAccessOwnProfileById() throws Exception {
        mockMvc.perform(get("/api/customers/" + customer1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customer1.getId()))
                .andExpect(jsonPath("$.email").value("customer1@logistics.com"));
    }

    @Test
    @WithMockUser(username = "customer1@logistics.com", roles = "CUSTOMER")
    void customer_cannotAccessAnotherCustomerProfile() throws Exception {
        // customer1 attempts to access customer2's profile by ID
        mockMvc.perform(get("/api/customers/" + customer2.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @WithMockUser(username = "admin@logistics.com", roles = "ADMIN")
    void admin_canAccessAnotherCustomerProfile() throws Exception {
        // admin accesses customer2's profile
        mockMvc.perform(get("/api/customers/" + customer2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customer2.getId()))
                .andExpect(jsonPath("$.email").value("customer2@logistics.com"));
    }

    @Test
    @WithMockUser(username = "customer1@logistics.com", roles = "CUSTOMER")
    void createCustomer_validationRejectsBlankRequiredFields() throws Exception {
        CreateCustomerRequest invalidRequest = new CreateCustomerRequest(
                "",
                "",
                "",
                ""
        );

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.phone").isNotEmpty())
                .andExpect(jsonPath("$.address").isNotEmpty())
                .andExpect(jsonPath("$.city").isNotEmpty())
                .andExpect(jsonPath("$.postalCode").isNotEmpty());
    }

    @Test
    void unauthenticatedUser_cannotAccessCustomerEndpoints() throws Exception {
        mockMvc.perform(get("/api/customers/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
