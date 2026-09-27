package com.ahmed.logistics.shipment.controller;

import com.ahmed.logistics.customer.entity.Customer;
import com.ahmed.logistics.customer.repository.CustomerRepository;
import com.ahmed.logistics.driver.entity.Driver;
import com.ahmed.logistics.driver.entity.DriverStatus;
import com.ahmed.logistics.driver.repository.DriverRepository;
import com.ahmed.logistics.shipment.dto.AssignShipmentRequest;
import com.ahmed.logistics.shipment.dto.CreateShipmentRequest;
import com.ahmed.logistics.shipment.dto.ShipmentResponse;
import com.ahmed.logistics.shipment.entity.Shipment;
import com.ahmed.logistics.shipment.entity.ShipmentStatus;
import com.ahmed.logistics.shipment.entity.ShipmentType;
import com.ahmed.logistics.shipment.repository.ShipmentRepository;
import com.ahmed.logistics.shipment.service.ShipmentService;
import com.ahmed.logistics.shipment.tracking.entity.ShipmentTracking;
import com.ahmed.logistics.shipment.tracking.repository.ShipmentTrackingRepository;
import com.ahmed.logistics.user.entity.Role;
import com.ahmed.logistics.user.entity.User;
import com.ahmed.logistics.user.repository.UserRepository;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class ShipmentAssignmentIntegrationTest {

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
    private ShipmentRepository shipmentRepository;

    @Autowired
    private ShipmentTrackingRepository shipmentTrackingRepository;

    @Autowired
    private ShipmentService shipmentService;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MockMvc mockMvc;

    private Customer customer;
    private Driver driver;
    private Vehicle vehicle;
    private Shipment shipment;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Customer
        User custUser = userRepository.saveAndFlush(User.builder()
                .email("asgn_cust@logistics.com")
                .password("$2a$10$hashedPass")
                .firstName("Customer")
                .lastName("User")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build());

        customer = customerRepository.saveAndFlush(Customer.builder()
                .user(custUser)
                .phone("+1234567800")
                .address("100 Market St")
                .city("Dallas")
                .postalCode("75001")
                .build());

        // Driver
        User driverUser = userRepository.saveAndFlush(User.builder()
                .email("asgn_driver@logistics.com")
                .password("$2a$10$hashedPass")
                .firstName("Bob")
                .lastName("Driver")
                .role(Role.DRIVER)
                .enabled(true)
                .build());

        driver = driverRepository.saveAndFlush(Driver.builder()
                .user(driverUser)
                .phone("+1999888777")
                .licenseNumber("DL-ASGN-100")
                .licenseExpiryDate(LocalDate.now().plusYears(3))
                .status(DriverStatus.AVAILABLE)
                .build());

        // Vehicle
        vehicle = vehicleRepository.saveAndFlush(Vehicle.builder()
                .plateNumber("TEX-1234")
                .type(VehicleType.TRUCK)
                .status(VehicleStatus.AVAILABLE)
                .brand("Volvo")
                .model("VNL")
                .manufacturingYear(2021)
                .maxWeightKg(10000.0)
                .build());

        // Shipment
        ShipmentResponse created = shipmentService.createShipment(new CreateShipmentRequest(
                customer.getId(),
                ShipmentType.STANDARD,
                "100 Market St",
                "Dallas",
                "75001",
                "200 Commerce St",
                "Houston",
                "77001",
                "Jane Doe",
                "+1555444333",
                "Pallet of Goods",
                50.0,
                50.0,
                50.0,
                50.0
        ));

        shipment = shipmentRepository.findById(created.id()).orElseThrow();
    }

    @Test
    @DisplayName("ADMIN role can successfully assign driver and vehicle to shipment")
    @WithMockUser(username = "admin@logistics.com", roles = {"ADMIN"})
    void assignDriverAndVehicle_asAdmin_success() throws Exception {
        AssignShipmentRequest request = new AssignShipmentRequest(driver.getId(), vehicle.getId());

        mockMvc.perform(patch("/api/shipments/{shipmentId}/assignment", shipment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(shipment.getId()))
                .andExpect(jsonPath("$.driver", notNullValue()))
                .andExpect(jsonPath("$.driver.id").value(driver.getId()))
                .andExpect(jsonPath("$.driver.name").value("Bob Driver"))
                .andExpect(jsonPath("$.driver.status").value("BUSY"))
                .andExpect(jsonPath("$.vehicle", notNullValue()))
                .andExpect(jsonPath("$.vehicle.id").value(vehicle.getId()))
                .andExpect(jsonPath("$.vehicle.plateNumber").value("TEX-1234"))
                .andExpect(jsonPath("$.vehicle.status").value("IN_USE"));

        // Verify Driver became BUSY in DB
        Driver updatedDriver = driverRepository.findById(driver.getId()).orElseThrow();
        assertEquals(DriverStatus.BUSY, updatedDriver.getStatus());

        // Verify Vehicle became IN_USE in DB
        Vehicle updatedVehicle = vehicleRepository.findById(vehicle.getId()).orElseThrow();
        assertEquals(VehicleStatus.IN_USE, updatedVehicle.getStatus());

        // Verify Tracking event appended
        List<ShipmentTracking> trackings = shipmentTrackingRepository.findByShipmentIdOrderByCreatedAtAsc(shipment.getId());
        assertEquals(2, trackings.size());
        assertEquals("Shipment created", trackings.get(0).getDescription());
        assertEquals("Driver and vehicle assigned", trackings.get(1).getDescription());
    }

    @Test
    @DisplayName("DISPATCHER role can successfully assign driver and vehicle to shipment")
    @WithMockUser(username = "disp@logistics.com", roles = {"DISPATCHER"})
    void assignDriverAndVehicle_asDispatcher_success() throws Exception {
        AssignShipmentRequest request = new AssignShipmentRequest(driver.getId(), vehicle.getId());

        mockMvc.perform(patch("/api/shipments/{shipmentId}/assignment", shipment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driver.status").value("BUSY"))
                .andExpect(jsonPath("$.vehicle.status").value("IN_USE"));
    }

    @Test
    @DisplayName("CUSTOMER role is forbidden from assigning driver and vehicle (403)")
    @WithMockUser(username = "asgn_cust@logistics.com", roles = {"CUSTOMER"})
    void assignDriverAndVehicle_asCustomer_forbidden() throws Exception {
        AssignShipmentRequest request = new AssignShipmentRequest(driver.getId(), vehicle.getId());

        mockMvc.perform(patch("/api/shipments/{shipmentId}/assignment", shipment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DRIVER role is forbidden from assigning driver and vehicle (403)")
    @WithMockUser(username = "asgn_driver@logistics.com", roles = {"DRIVER"})
    void assignDriverAndVehicle_asDriver_forbidden() throws Exception {
        AssignShipmentRequest request = new AssignShipmentRequest(driver.getId(), vehicle.getId());

        mockMvc.perform(patch("/api/shipments/{shipmentId}/assignment", shipment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request to assignment returns 401")
    void assignDriverAndVehicle_unauthenticated_returns401() throws Exception {
        AssignShipmentRequest request = new AssignShipmentRequest(driver.getId(), vehicle.getId());

        mockMvc.perform(patch("/api/shipments/{shipmentId}/assignment", shipment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Assigning when driver is not AVAILABLE returns 400 Bad Request")
    @WithMockUser(username = "admin@logistics.com", roles = {"ADMIN"})
    void assignDriverAndVehicle_driverNotAvailable_returns400() throws Exception {
        driver.setStatus(DriverStatus.OFFLINE);
        driverRepository.saveAndFlush(driver);

        AssignShipmentRequest request = new AssignShipmentRequest(driver.getId(), vehicle.getId());

        mockMvc.perform(patch("/api/shipments/{shipmentId}/assignment", shipment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Driver is not available for assignment. Current status: OFFLINE")));
    }

    @Test
    @DisplayName("Assigning when vehicle is not AVAILABLE returns 400 Bad Request")
    @WithMockUser(username = "admin@logistics.com", roles = {"ADMIN"})
    void assignDriverAndVehicle_vehicleNotAvailable_returns400() throws Exception {
        vehicle.setStatus(VehicleStatus.MAINTENANCE);
        vehicleRepository.saveAndFlush(vehicle);

        AssignShipmentRequest request = new AssignShipmentRequest(driver.getId(), vehicle.getId());

        mockMvc.perform(patch("/api/shipments/{shipmentId}/assignment", shipment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Vehicle is not available for assignment. Current status: MAINTENANCE")));
    }

    @Test
    @DisplayName("Assigning to shipment that already has assignment returns 400 Bad Request")
    @WithMockUser(username = "admin@logistics.com", roles = {"ADMIN"})
    void assignDriverAndVehicle_alreadyAssigned_returns400() throws Exception {
        AssignShipmentRequest request = new AssignShipmentRequest(driver.getId(), vehicle.getId());

        // First assignment succeeds
        mockMvc.perform(patch("/api/shipments/{shipmentId}/assignment", shipment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Second assignment fails
        mockMvc.perform(patch("/api/shipments/{shipmentId}/assignment", shipment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Shipment already has an assigned driver or vehicle")));
    }

    @Test
    @DisplayName("Assigning when shipment is in CANCELLED status returns 400 Bad Request")
    @WithMockUser(username = "admin@logistics.com", roles = {"ADMIN"})
    void assignDriverAndVehicle_cancelledShipment_returns400() throws Exception {
        shipment.setStatus(ShipmentStatus.CANCELLED);
        shipmentRepository.saveAndFlush(shipment);

        AssignShipmentRequest request = new AssignShipmentRequest(driver.getId(), vehicle.getId());

        mockMvc.perform(patch("/api/shipments/{shipmentId}/assignment", shipment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Shipment cannot be assigned in status: CANCELLED")));
    }

    @Test
    @DisplayName("Assigning non-existent shipment returns 404 Not Found")
    @WithMockUser(username = "admin@logistics.com", roles = {"ADMIN"})
    void assignDriverAndVehicle_shipmentNotFound_returns404() throws Exception {
        AssignShipmentRequest request = new AssignShipmentRequest(driver.getId(), vehicle.getId());

        mockMvc.perform(patch("/api/shipments/{shipmentId}/assignment", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Assigning non-existent driver returns 404 Not Found")
    @WithMockUser(username = "admin@logistics.com", roles = {"ADMIN"})
    void assignDriverAndVehicle_driverNotFound_returns404() throws Exception {
        AssignShipmentRequest request = new AssignShipmentRequest(999999L, vehicle.getId());

        mockMvc.perform(patch("/api/shipments/{shipmentId}/assignment", shipment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Assigning non-existent vehicle returns 404 Not Found")
    @WithMockUser(username = "admin@logistics.com", roles = {"ADMIN"})
    void assignDriverAndVehicle_vehicleNotFound_returns404() throws Exception {
        AssignShipmentRequest request = new AssignShipmentRequest(driver.getId(), 999999L);

        mockMvc.perform(patch("/api/shipments/{shipmentId}/assignment", shipment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Null driverId in request returns 400 Bad Request")
    @WithMockUser(username = "admin@logistics.com", roles = {"ADMIN"})
    void assignDriverAndVehicle_nullDriverId_returns400() throws Exception {
        AssignShipmentRequest request = new AssignShipmentRequest(null, vehicle.getId());

        mockMvc.perform(patch("/api/shipments/{shipmentId}/assignment", shipment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.driverId", is("Driver ID is required")));
    }
}
