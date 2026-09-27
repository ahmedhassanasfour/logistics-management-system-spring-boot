package com.ahmed.logistics.branch.controller;

import com.ahmed.logistics.branch.dto.CreateBranchRequest;
import com.ahmed.logistics.branch.dto.UpdateBranchRequest;
import com.ahmed.logistics.branch.entity.Branch;
import com.ahmed.logistics.branch.entity.BranchStatus;
import com.ahmed.logistics.branch.repository.BranchRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class BranchControllerSecurityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BranchRepository branchRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MockMvc mockMvc;

    private Branch sampleBranch;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Create Admin user
        User admin = User.builder()
                .email("admin_br_test@logistics.com")
                .password("$2a$10$hashedPass")
                .firstName("Admin")
                .lastName("User")
                .role(Role.ADMIN)
                .enabled(true)
                .build();
        userRepository.saveAndFlush(admin);

        // Create Customer user
        User customer = User.builder()
                .email("customer_br_test@logistics.com")
                .password("$2a$10$hashedPass")
                .firstName("Customer")
                .lastName("User")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();
        userRepository.saveAndFlush(customer);

        // Create Driver user
        User driver = User.builder()
                .email("driver_br_test@logistics.com")
                .password("$2a$10$hashedPass")
                .firstName("Driver")
                .lastName("User")
                .role(Role.DRIVER)
                .enabled(true)
                .build();
        userRepository.saveAndFlush(driver);

        // Create Dispatcher user
        User dispatcher = User.builder()
                .email("dispatcher_br_test@logistics.com")
                .password("$2a$10$hashedPass")
                .firstName("Dispatcher")
                .lastName("User")
                .role(Role.DISPATCHER)
                .enabled(true)
                .build();
        userRepository.saveAndFlush(dispatcher);

        // Create sample branch
        sampleBranch = Branch.builder()
                .name("Houston Central Branch")
                .code("BR-HOU-001")
                .address("500 Texas Ave")
                .city("Houston")
                .postalCode("77002")
                .phone("+17135550199")
                .email("houston-central@logistics.com")
                .status(BranchStatus.ACTIVE)
                .build();
        sampleBranch = branchRepository.saveAndFlush(sampleBranch);
    }

    @Test
    void unauthenticated_getReturns401() throws Exception {
        mockMvc.perform(get("/api/branches/" + sampleBranch.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void unauthenticated_postReturns401() throws Exception {
        CreateBranchRequest request = new CreateBranchRequest(
                "Test Branch",
                "BR-TEST-001",
                "123 Test St",
                "Dallas",
                "75201",
                "+12145550111",
                "test@logistics.com"
        );

        mockMvc.perform(post("/api/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(username = "admin_br_test@logistics.com", roles = "ADMIN")
    void admin_canCreateBranch() throws Exception {
        CreateBranchRequest request = new CreateBranchRequest(
                "Dallas West Branch",
                "BR-DAL-002",
                "800 Elm St",
                "Dallas",
                "75202",
                "+12145550222",
                "dallas-west@logistics.com"
        );

        mockMvc.perform(post("/api/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Dallas West Branch"))
                .andExpect(jsonPath("$.code").value("BR-DAL-002"))
                .andExpect(jsonPath("$.address").value("800 Elm St"))
                .andExpect(jsonPath("$.city").value("Dallas"))
                .andExpect(jsonPath("$.postalCode").value("75202"))
                .andExpect(jsonPath("$.phone").value("+12145550222"))
                .andExpect(jsonPath("$.email").value("dallas-west@logistics.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(username = "admin_br_test@logistics.com", roles = "ADMIN")
    void admin_canUpdateBranch() throws Exception {
        UpdateBranchRequest updateRequest = new UpdateBranchRequest(
                "Houston Downtown Branch",
                "BR-HOU-001-MOD",
                "500 Texas Ave Suite 200",
                "Houston",
                "77002",
                "+17135550299",
                "houston-mod@logistics.com"
        );

        mockMvc.perform(put("/api/branches/" + sampleBranch.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sampleBranch.getId()))
                .andExpect(jsonPath("$.name").value("Houston Downtown Branch"))
                .andExpect(jsonPath("$.code").value("BR-HOU-001-MOD"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(username = "admin_br_test@logistics.com", roles = "ADMIN")
    void admin_canDeleteBranch() throws Exception {
        mockMvc.perform(delete("/api/branches/" + sampleBranch.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin_br_test@logistics.com", roles = "ADMIN")
    void admin_canReadBranchById() throws Exception {
        mockMvc.perform(get("/api/branches/" + sampleBranch.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sampleBranch.getId()))
                .andExpect(jsonPath("$.code").value("BR-HOU-001"))
                .andExpect(jsonPath("$.name").value("Houston Central Branch"));
    }

    @Test
    @WithMockUser(username = "admin_br_test@logistics.com", roles = "ADMIN")
    void admin_canReadBranchByCode() throws Exception {
        mockMvc.perform(get("/api/branches/code/" + sampleBranch.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sampleBranch.getId()))
                .andExpect(jsonPath("$.code").value("BR-HOU-001"));
    }

    @Test
    @WithMockUser(username = "customer_br_test@logistics.com", roles = "CUSTOMER")
    void customer_canReadBranch() throws Exception {
        mockMvc.perform(get("/api/branches/" + sampleBranch.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sampleBranch.getId()))
                .andExpect(jsonPath("$.code").value("BR-HOU-001"));
    }

    @Test
    @WithMockUser(username = "driver_br_test@logistics.com", roles = "DRIVER")
    void driver_canReadBranch() throws Exception {
        mockMvc.perform(get("/api/branches/code/" + sampleBranch.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sampleBranch.getId()))
                .andExpect(jsonPath("$.code").value("BR-HOU-001"));
    }

    @Test
    @WithMockUser(username = "dispatcher_br_test@logistics.com", roles = "DISPATCHER")
    void dispatcher_canReadBranch() throws Exception {
        mockMvc.perform(get("/api/branches/" + sampleBranch.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sampleBranch.getId()))
                .andExpect(jsonPath("$.code").value("BR-HOU-001"));
    }

    @Test
    @WithMockUser(username = "customer_br_test@logistics.com", roles = "CUSTOMER")
    void customer_cannotCreateUpdateOrDeleteBranch() throws Exception {
        CreateBranchRequest createReq = new CreateBranchRequest(
                "Cust Branch", "BR-CUST", "Addr", "City", "00000", "+123456789", "c@log.com"
        );
        mockMvc.perform(post("/api/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        UpdateBranchRequest updateReq = new UpdateBranchRequest(
                "Cust Branch", "BR-CUST", "Addr", "City", "00000", "+123456789", "c@log.com"
        );
        mockMvc.perform(put("/api/branches/" + sampleBranch.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(delete("/api/branches/" + sampleBranch.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "driver_br_test@logistics.com", roles = "DRIVER")
    void driver_cannotCreateUpdateOrDeleteBranch() throws Exception {
        CreateBranchRequest createReq = new CreateBranchRequest(
                "Driver Branch", "BR-DRV", "Addr", "City", "00000", "+123456789", "d@log.com"
        );
        mockMvc.perform(post("/api/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        UpdateBranchRequest updateReq = new UpdateBranchRequest(
                "Driver Branch", "BR-DRV", "Addr", "City", "00000", "+123456789", "d@log.com"
        );
        mockMvc.perform(put("/api/branches/" + sampleBranch.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(delete("/api/branches/" + sampleBranch.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "dispatcher_br_test@logistics.com", roles = "DISPATCHER")
    void dispatcher_cannotCreateUpdateOrDeleteBranch() throws Exception {
        CreateBranchRequest createReq = new CreateBranchRequest(
                "Disp Branch", "BR-DSP", "Addr", "City", "00000", "+123456789", "dsp@log.com"
        );
        mockMvc.perform(post("/api/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        UpdateBranchRequest updateReq = new UpdateBranchRequest(
                "Disp Branch", "BR-DSP", "Addr", "City", "00000", "+123456789", "dsp@log.com"
        );
        mockMvc.perform(put("/api/branches/" + sampleBranch.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(delete("/api/branches/" + sampleBranch.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "admin_br_test@logistics.com", roles = "ADMIN")
    void createBranch_validationRejectsInvalidRequest() throws Exception {
        CreateBranchRequest invalidRequest = new CreateBranchRequest(
                "",               // blank name
                "",               // blank code
                "",               // blank address
                "",               // blank city
                "",               // blank postal code
                "",               // blank phone
                "not-an-email"    // invalid email
        );

        mockMvc.perform(post("/api/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").isNotEmpty())
                .andExpect(jsonPath("$.code").isNotEmpty())
                .andExpect(jsonPath("$.address").isNotEmpty())
                .andExpect(jsonPath("$.city").isNotEmpty())
                .andExpect(jsonPath("$.postalCode").isNotEmpty())
                .andExpect(jsonPath("$.phone").isNotEmpty())
                .andExpect(jsonPath("$.email").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "admin_br_test@logistics.com", roles = "ADMIN")
    void createBranch_duplicateCodeReturns400() throws Exception {
        CreateBranchRequest duplicateRequest = new CreateBranchRequest(
                "Different Name",
                "BR-HOU-001", // duplicate code
                "123 Some Other Way",
                "Houston",
                "77002",
                "+17135559999",
                "dup@logistics.com"
        );

        mockMvc.perform(post("/api/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already exists")));
    }
}
