package com.ahmed.logistics.shipment.service;

import com.ahmed.logistics.customer.entity.Customer;
import com.ahmed.logistics.driver.entity.Driver;
import com.ahmed.logistics.driver.entity.DriverStatus;
import com.ahmed.logistics.driver.repository.DriverRepository;
import com.ahmed.logistics.exception.BadRequestException;
import com.ahmed.logistics.exception.ResourceNotFoundException;
import com.ahmed.logistics.shipment.dto.ShipmentResponse;
import com.ahmed.logistics.shipment.entity.Shipment;
import com.ahmed.logistics.shipment.entity.ShipmentStatus;
import com.ahmed.logistics.shipment.entity.ShipmentType;
import com.ahmed.logistics.shipment.repository.ShipmentRepository;
import com.ahmed.logistics.shipment.tracking.service.ShipmentTrackingService;
import com.ahmed.logistics.user.entity.Role;
import com.ahmed.logistics.user.entity.User;
import com.ahmed.logistics.vehicle.entity.Vehicle;
import com.ahmed.logistics.vehicle.entity.VehicleStatus;
import com.ahmed.logistics.vehicle.entity.VehicleType;
import com.ahmed.logistics.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentAssignmentServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private ShipmentTrackingService shipmentTrackingService;

    @InjectMocks
    private ShipmentAssignmentService assignmentService;

    private Shipment sampleShipment;
    private Driver sampleDriver;
    private Vehicle sampleVehicle;

    @BeforeEach
    void setUp() {
        User customerUser = User.builder()
                .id(1L)
                .email("cust@logistics.com")
                .firstName("John")
                .lastName("Doe")
                .role(Role.CUSTOMER)
                .build();

        Customer customer = Customer.builder()
                .id(10L)
                .user(customerUser)
                .phone("+123456789")
                .address("100 Main St")
                .city("Boston")
                .postalCode("02101")
                .build();

        sampleShipment = Shipment.builder()
                .id(50L)
                .trackingNumber("SHP-TEST1234")
                .customer(customer)
                .status(ShipmentStatus.CREATED)
                .shipmentType(ShipmentType.STANDARD)
                .pickupAddress("100 Main St")
                .pickupCity("Boston")
                .pickupPostalCode("02101")
                .deliveryAddress("200 Elm St")
                .deliveryCity("New York")
                .deliveryPostalCode("10001")
                .recipientName("Alice")
                .recipientPhone("+198765432")
                .weightKg(5.0)
                .basePrice(new BigDecimal("10.00"))
                .shippingFee(new BigDecimal("5.00"))
                .totalPrice(new BigDecimal("15.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User driverUser = User.builder()
                .id(2L)
                .email("driver@logistics.com")
                .firstName("Sam")
                .lastName("Driver")
                .role(Role.DRIVER)
                .build();

        sampleDriver = Driver.builder()
                .id(100L)
                .user(driverUser)
                .phone("+1122334455")
                .licenseNumber("DL-12345678")
                .licenseExpiryDate(LocalDate.now().plusYears(2))
                .status(DriverStatus.AVAILABLE)
                .build();

        sampleVehicle = Vehicle.builder()
                .id(200L)
                .plateNumber("XYZ-9999")
                .type(VehicleType.VAN)
                .status(VehicleStatus.AVAILABLE)
                .brand("Ford")
                .model("Transit")
                .manufacturingYear(2022)
                .maxWeightKg(1500.0)
                .build();
    }

    @Test
    @DisplayName("assignDriverAndVehicle successfully assigns driver and vehicle, updates statuses, and records tracking")
    void assignDriverAndVehicle_success() {
        when(shipmentRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(sampleShipment));
        when(driverRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(sampleDriver));
        when(vehicleRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(sampleVehicle));
        when(shipmentRepository.existsByDriverIdAndStatusIn(eq(100L), any())).thenReturn(false);
        when(shipmentRepository.existsByVehicleIdAndStatusIn(eq(200L), any())).thenReturn(false);
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShipmentResponse response = assignmentService.assignDriverAndVehicle(50L, 100L, 200L);

        assertNotNull(response);
        assertEquals(DriverStatus.BUSY, sampleDriver.getStatus());
        assertEquals(VehicleStatus.IN_USE, sampleVehicle.getStatus());
        assertEquals(sampleDriver, sampleShipment.getDriver());
        assertEquals(sampleVehicle, sampleShipment.getVehicle());

        assertNotNull(response.driver());
        assertEquals(100L, response.driver().id());
        assertEquals("Sam Driver", response.driver().name());
        assertEquals(DriverStatus.BUSY, response.driver().status());

        assertNotNull(response.vehicle());
        assertEquals(200L, response.vehicle().id());
        assertEquals("XYZ-9999", response.vehicle().plateNumber());
        assertEquals(VehicleStatus.IN_USE, response.vehicle().status());

        verify(driverRepository).save(sampleDriver);
        verify(vehicleRepository).save(sampleVehicle);
        verify(shipmentRepository).save(sampleShipment);
        verify(shipmentTrackingService).recordStatusChange(
                sampleShipment,
                ShipmentStatus.CREATED,
                "Driver and vehicle assigned",
                null
        );
    }

    @Test
    @DisplayName("assignDriverAndVehicle throws BadRequestException when arguments are null")
    void assignDriverAndVehicle_nullArguments_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> assignmentService.assignDriverAndVehicle(null, 100L, 200L));
        assertThrows(BadRequestException.class, () -> assignmentService.assignDriverAndVehicle(50L, null, 200L));
        assertThrows(BadRequestException.class, () -> assignmentService.assignDriverAndVehicle(50L, 100L, null));
    }

    @Test
    @DisplayName("assignDriverAndVehicle throws ResourceNotFoundException when shipment does not exist")
    void assignDriverAndVehicle_shipmentNotFound_throwsResourceNotFoundException() {
        when(shipmentRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                assignmentService.assignDriverAndVehicle(999L, 100L, 200L));
    }

    @ParameterizedTest(name = "Non-assignable status: {0}")
    @EnumSource(value = ShipmentStatus.class, names = {
            "PICKED_UP", "IN_TRANSIT", "OUT_FOR_DELIVERY", "DELIVERED",
            "CANCELLED", "DELIVERY_FAILED", "RESCHEDULED", "RETURNED"
    })
    @DisplayName("assignDriverAndVehicle throws BadRequestException when shipment is in non-assignable status")
    void assignDriverAndVehicle_nonAssignableStatus_throwsBadRequestException(ShipmentStatus status) {
        sampleShipment.setStatus(status);
        when(shipmentRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(sampleShipment));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                assignmentService.assignDriverAndVehicle(50L, 100L, 200L));

        assertTrue(ex.getMessage().contains("Shipment cannot be assigned in status: " + status));
        verify(driverRepository, never()).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("assignDriverAndVehicle throws BadRequestException when shipment already has a driver or vehicle")
    void assignDriverAndVehicle_alreadyAssigned_throwsBadRequestException() {
        sampleShipment.setDriver(sampleDriver);
        when(shipmentRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(sampleShipment));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                assignmentService.assignDriverAndVehicle(50L, 100L, 200L));

        assertTrue(ex.getMessage().contains("already has an assigned driver or vehicle"));
        verify(driverRepository, never()).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("assignDriverAndVehicle throws ResourceNotFoundException when driver does not exist")
    void assignDriverAndVehicle_driverNotFound_throwsResourceNotFoundException() {
        when(shipmentRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(sampleShipment));
        when(driverRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                assignmentService.assignDriverAndVehicle(50L, 999L, 200L));
    }

    @Test
    @DisplayName("assignDriverAndVehicle throws BadRequestException when driver user role is not DRIVER")
    void assignDriverAndVehicle_driverUserNotDriverRole_throwsBadRequestException() {
        sampleDriver.getUser().setRole(Role.CUSTOMER);
        when(shipmentRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(sampleShipment));
        when(driverRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(sampleDriver));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                assignmentService.assignDriverAndVehicle(50L, 100L, 200L));

        assertTrue(ex.getMessage().contains("not a valid driver"));
    }

    @ParameterizedTest(name = "Non-available driver status: {0}")
    @EnumSource(value = DriverStatus.class, names = {"BUSY", "OFFLINE", "SUSPENDED"})
    @DisplayName("assignDriverAndVehicle throws BadRequestException when driver status is not AVAILABLE")
    void assignDriverAndVehicle_driverNotAvailable_throwsBadRequestException(DriverStatus status) {
        sampleDriver.setStatus(status);
        when(shipmentRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(sampleShipment));
        when(driverRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(sampleDriver));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                assignmentService.assignDriverAndVehicle(50L, 100L, 200L));

        assertTrue(ex.getMessage().contains("Driver is not available for assignment"));
    }

    @Test
    @DisplayName("assignDriverAndVehicle throws ResourceNotFoundException when vehicle does not exist")
    void assignDriverAndVehicle_vehicleNotFound_throwsResourceNotFoundException() {
        when(shipmentRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(sampleShipment));
        when(driverRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(sampleDriver));
        when(vehicleRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                assignmentService.assignDriverAndVehicle(50L, 100L, 999L));
    }

    @ParameterizedTest(name = "Non-available vehicle status: {0}")
    @EnumSource(value = VehicleStatus.class, names = {"IN_USE", "MAINTENANCE", "OUT_OF_SERVICE"})
    @DisplayName("assignDriverAndVehicle throws BadRequestException when vehicle status is not AVAILABLE")
    void assignDriverAndVehicle_vehicleNotAvailable_throwsBadRequestException(VehicleStatus status) {
        sampleVehicle.setStatus(status);
        when(shipmentRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(sampleShipment));
        when(driverRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(sampleDriver));
        when(vehicleRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(sampleVehicle));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                assignmentService.assignDriverAndVehicle(50L, 100L, 200L));

        assertTrue(ex.getMessage().contains("Vehicle is not available for assignment"));
    }

    @Test
    @DisplayName("assignDriverAndVehicle throws BadRequestException when driver is already assigned to active shipment")
    void assignDriverAndVehicle_driverAlreadyActive_throwsBadRequestException() {
        when(shipmentRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(sampleShipment));
        when(driverRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(sampleDriver));
        when(vehicleRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(sampleVehicle));
        when(shipmentRepository.existsByDriverIdAndStatusIn(eq(100L), any())).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                assignmentService.assignDriverAndVehicle(50L, 100L, 200L));

        assertTrue(ex.getMessage().contains("Driver is already assigned to an active shipment"));
        verify(driverRepository, never()).save(any());
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @DisplayName("assignDriverAndVehicle throws BadRequestException when vehicle is already assigned to active shipment")
    void assignDriverAndVehicle_vehicleAlreadyActive_throwsBadRequestException() {
        when(shipmentRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(sampleShipment));
        when(driverRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(sampleDriver));
        when(vehicleRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(sampleVehicle));
        when(shipmentRepository.existsByDriverIdAndStatusIn(eq(100L), any())).thenReturn(false);
        when(shipmentRepository.existsByVehicleIdAndStatusIn(eq(200L), any())).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                assignmentService.assignDriverAndVehicle(50L, 100L, 200L));

        assertTrue(ex.getMessage().contains("Vehicle is already assigned to an active shipment"));
        verify(driverRepository, never()).save(any());
        verify(vehicleRepository, never()).save(any());
    }
}
