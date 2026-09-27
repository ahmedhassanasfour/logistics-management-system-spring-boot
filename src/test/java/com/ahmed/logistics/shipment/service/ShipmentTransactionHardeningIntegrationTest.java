package com.ahmed.logistics.shipment.service;

import com.ahmed.logistics.customer.entity.Customer;
import com.ahmed.logistics.customer.repository.CustomerRepository;
import com.ahmed.logistics.driver.entity.Driver;
import com.ahmed.logistics.driver.entity.DriverStatus;
import com.ahmed.logistics.driver.repository.DriverRepository;
import com.ahmed.logistics.exception.BadRequestException;
import com.ahmed.logistics.exception.ResourceNotFoundException;
import com.ahmed.logistics.shipment.dto.CreateShipmentRequest;
import com.ahmed.logistics.shipment.dto.ShipmentResponse;
import com.ahmed.logistics.shipment.entity.Shipment;
import com.ahmed.logistics.shipment.entity.ShipmentStatus;
import com.ahmed.logistics.shipment.entity.ShipmentType;
import com.ahmed.logistics.shipment.repository.ShipmentRepository;
import com.ahmed.logistics.shipment.tracking.entity.ShipmentTracking;
import com.ahmed.logistics.shipment.tracking.repository.ShipmentTrackingRepository;
import com.ahmed.logistics.user.entity.Role;
import com.ahmed.logistics.user.entity.User;
import com.ahmed.logistics.user.repository.UserRepository;
import com.ahmed.logistics.vehicle.entity.Vehicle;
import com.ahmed.logistics.vehicle.entity.VehicleStatus;
import com.ahmed.logistics.vehicle.entity.VehicleType;
import com.ahmed.logistics.vehicle.repository.VehicleRepository;
import com.ahmed.logistics.warehouse.entity.Warehouse;
import com.ahmed.logistics.warehouse.entity.WarehouseStatus;
import com.ahmed.logistics.warehouse.movement.entity.WarehouseMovement;
import com.ahmed.logistics.warehouse.movement.repository.WarehouseMovementRepository;
import com.ahmed.logistics.warehouse.movement.service.WarehouseMovementService;
import com.ahmed.logistics.warehouse.repository.WarehouseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ShipmentTransactionHardeningIntegrationTest {

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private ShipmentLifecycleService shipmentLifecycleService;

    @Autowired
    private ShipmentAssignmentService shipmentAssignmentService;

    @Autowired
    private WarehouseMovementService warehouseMovementService;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private ShipmentTrackingRepository shipmentTrackingRepository;

    @Autowired
    private WarehouseMovementRepository warehouseMovementRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    private Customer testCustomer;
    private String uniqueSuffix;

    @BeforeEach
    void setUp() {
        uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);

        User custUser = userRepository.saveAndFlush(User.builder()
                .email("tx_cust_" + uniqueSuffix + "@logistics.com")
                .password("$2a$10$hashedPass")
                .firstName("Tx")
                .lastName("Customer")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build());

        testCustomer = customerRepository.saveAndFlush(Customer.builder()
                .user(custUser)
                .phone("+1555" + uniqueSuffix)
                .address("100 Tx Blvd")
                .city("Atlanta")
                .postalCode("30301")
                .build());
    }

    @AfterEach
    void tearDown() {
        warehouseMovementRepository.deleteAll();
        shipmentTrackingRepository.deleteAll();
        shipmentRepository.deleteAll();
        driverRepository.deleteAll();
        vehicleRepository.deleteAll();
        warehouseRepository.deleteAll();
        customerRepository.deleteAll();
        userRepository.deleteAll();
    }

    private Shipment createTestShipment() {
        CreateShipmentRequest request = new CreateShipmentRequest(
                testCustomer.getId(),
                ShipmentType.STANDARD,
                "100 Start Rd", "Atlanta", "30301",
                "200 End Rd", "Miami", "33101",
                "Recipient " + uniqueSuffix,
                "+1555987654",
                "Electronics",
                10.0, 30.0, 30.0, 30.0
        );
        ShipmentResponse response = shipmentService.createShipment(request);
        return shipmentRepository.findById(response.id()).orElseThrow();
    }

    private Driver createTestDriver(String emailPrefix, DriverStatus status) {
        User driverUser = userRepository.saveAndFlush(User.builder()
                .email(emailPrefix + "_" + uniqueSuffix + "@logistics.com")
                .password("$2a$10$hashedPass")
                .firstName("Driver")
                .lastName("Tx")
                .role(Role.DRIVER)
                .enabled(true)
                .build());

        return driverRepository.saveAndFlush(Driver.builder()
                .user(driverUser)
                .licenseNumber("DL-" + UUID.randomUUID().toString().substring(0, 8))
                .licenseExpiryDate(LocalDate.now().plusYears(2))
                .status(status)
                .phone("+1555" + UUID.randomUUID().toString().substring(0, 6))
                .build());
    }

    private Vehicle createTestVehicle(VehicleStatus status) {
        return vehicleRepository.saveAndFlush(Vehicle.builder()
                .plateNumber("PL-" + UUID.randomUUID().toString().substring(0, 7))
                .type(VehicleType.VAN)
                .brand("Ford")
                .model("Transit")
                .manufacturingYear(2023)
                .maxWeightKg(1000.0)
                .status(status)
                .build());
    }

    private Warehouse createTestWarehouse(String name, WarehouseStatus status) {
        return warehouseRepository.saveAndFlush(Warehouse.builder()
                .name(name + " " + uniqueSuffix)
                .address("123 Logistics Way")
                .city("Atlanta")
                .postalCode("30301")
                .phone("+1555" + UUID.randomUUID().toString().substring(0, 6))
                .email("wh_" + uniqueSuffix + "@logistics.com")
                .status(status)
                .build());
    }

    // ==========================================
    // 1. Shipment Assignment Transaction Rollback
    // ==========================================

    @Test
    @DisplayName("Shipment assignment rolls back completely when vehicle does not exist")
    void assignDriverAndVehicle_vehicleNotFound_rollsBackDriverStatusAndShipment() {
        Shipment shipment = createTestShipment();
        Driver driver = createTestDriver("d_rollback_1", DriverStatus.AVAILABLE);
        Long nonExistentVehicleId = 999999L;

        assertThrows(ResourceNotFoundException.class, () ->
                shipmentAssignmentService.assignDriverAndVehicle(shipment.getId(), driver.getId(), nonExistentVehicleId));

        // Verify Driver remains AVAILABLE
        Driver refreshedDriver = driverRepository.findById(driver.getId()).orElseThrow();
        assertEquals(DriverStatus.AVAILABLE, refreshedDriver.getStatus());

        // Verify Shipment remains unassigned
        Shipment refreshedShipment = shipmentRepository.findById(shipment.getId()).orElseThrow();
        assertNull(refreshedShipment.getDriver());
        assertNull(refreshedShipment.getVehicle());

        // Verify no assignment tracking record was created (only initial CREATED tracking exists)
        List<ShipmentTracking> trackingEvents = shipmentTrackingRepository.findByShipmentIdOrderByCreatedAtAsc(shipment.getId());
        assertEquals(1, trackingEvents.size());
        assertEquals(ShipmentStatus.CREATED, trackingEvents.get(0).getStatus());
    }

    @Test
    @DisplayName("Shipment assignment rolls back completely when driver is not available")
    void assignDriverAndVehicle_driverNotAvailable_rollsBackVehicleStatusAndShipment() {
        Shipment shipment = createTestShipment();
        Driver driver = createTestDriver("d_rollback_2", DriverStatus.BUSY);
        Vehicle vehicle = createTestVehicle(VehicleStatus.AVAILABLE);

        assertThrows(BadRequestException.class, () ->
                shipmentAssignmentService.assignDriverAndVehicle(shipment.getId(), driver.getId(), vehicle.getId()));

        // Verify Vehicle remains AVAILABLE
        Vehicle refreshedVehicle = vehicleRepository.findById(vehicle.getId()).orElseThrow();
        assertEquals(VehicleStatus.AVAILABLE, refreshedVehicle.getStatus());

        // Verify Shipment remains unassigned
        Shipment refreshedShipment = shipmentRepository.findById(shipment.getId()).orElseThrow();
        assertNull(refreshedShipment.getDriver());
        assertNull(refreshedShipment.getVehicle());
    }

    // ==========================================
    // 2. Warehouse Movement Transaction Rollback
    // ==========================================

    @Test
    @DisplayName("Warehouse movement rolls back completely when target warehouse is not ACTIVE")
    void moveShipmentToWarehouse_inactiveWarehouse_rollsBackShipmentAndMovement() {
        Shipment shipment = createTestShipment();
        Warehouse inactiveWarehouse = createTestWarehouse("Inactive WH", WarehouseStatus.INACTIVE);

        assertThrows(BadRequestException.class, () ->
                warehouseMovementService.moveShipmentToWarehouse(shipment.getId(), inactiveWarehouse.getId(), "Movement test"));

        // Verify shipment currentWarehouse is still null
        Shipment refreshedShipment = shipmentRepository.findById(shipment.getId()).orElseThrow();
        assertNull(refreshedShipment.getCurrentWarehouse());

        // Verify no movement record exists
        List<WarehouseMovement> movements = warehouseMovementRepository.findByShipmentIdOrderByMovedAtAsc(shipment.getId());
        assertTrue(movements.isEmpty());

        // Verify no movement tracking record exists
        List<ShipmentTracking> trackingEvents = shipmentTrackingRepository.findByShipmentIdOrderByCreatedAtAsc(shipment.getId());
        assertEquals(1, trackingEvents.size());
    }

    @Test
    @DisplayName("Warehouse movement rolls back completely when moving to currently assigned warehouse")
    void moveShipmentToWarehouse_alreadyAtWarehouse_rollsBack() {
        Shipment shipment = createTestShipment();
        Warehouse warehouseA = createTestWarehouse("Warehouse Alpha", WarehouseStatus.ACTIVE);

        // First movement succeeds
        warehouseMovementService.moveShipmentToWarehouse(shipment.getId(), warehouseA.getId(), "Initial movement");

        Shipment afterFirstMove = shipmentRepository.findById(shipment.getId()).orElseThrow();
        assertEquals(warehouseA.getId(), afterFirstMove.getCurrentWarehouse().getId());

        // Second movement to the same warehouse fails
        assertThrows(BadRequestException.class, () ->
                warehouseMovementService.moveShipmentToWarehouse(shipment.getId(), warehouseA.getId(), "Duplicate movement"));

        // Verify only 1 movement record exists
        List<WarehouseMovement> movements = warehouseMovementRepository.findByShipmentIdOrderByMovedAtAsc(shipment.getId());
        assertEquals(1, movements.size());
    }

    // ==========================================
    // 3. Shipment Lifecycle Transaction Rollback
    // ==========================================

    @Test
    @DisplayName("Shipment lifecycle rolls back completely on invalid status transition")
    void transitionStatus_invalidTransition_rollsBackStatusAndTracking() {
        Shipment shipment = createTestShipment();
        assertEquals(ShipmentStatus.CREATED, shipment.getStatus());

        // Invalid transition: CREATED -> IN_TRANSIT
        assertThrows(BadRequestException.class, () ->
                shipmentLifecycleService.transitionStatus(shipment.getId(), ShipmentStatus.IN_TRANSIT));

        // Verify status remains CREATED
        Shipment refreshedShipment = shipmentRepository.findById(shipment.getId()).orElseThrow();
        assertEquals(ShipmentStatus.CREATED, refreshedShipment.getStatus());

        // Verify no new tracking record was added
        List<ShipmentTracking> trackingEvents = shipmentTrackingRepository.findByShipmentIdOrderByCreatedAtAsc(shipment.getId());
        assertEquals(1, trackingEvents.size());
    }

    // ==========================================
    // 4. Concurrency Protection: Driver Assignment
    // ==========================================

    @Test
    @DisplayName("Concurrent assignment of the same driver to two shipments allows only one to succeed")
    void concurrentAssignment_sameDriver_onlyOneSucceeds() throws InterruptedException {
        Shipment shipment1 = createTestShipment();
        Shipment shipment2 = createTestShipment();
        Driver driver = createTestDriver("d_concurrent", DriverStatus.AVAILABLE);
        Vehicle vehicle1 = createTestVehicle(VehicleStatus.AVAILABLE);
        Vehicle vehicle2 = createTestVehicle(VehicleStatus.AVAILABLE);

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        executor.submit(() -> {
            try {
                startLatch.await();
                shipmentAssignmentService.assignDriverAndVehicle(shipment1.getId(), driver.getId(), vehicle1.getId());
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                shipmentAssignmentService.assignDriverAndVehicle(shipment2.getId(), driver.getId(), vehicle2.getId());
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished, "Concurrent execution timed out");
        assertEquals(1, successCount.get(), "Exactly one assignment must succeed");
        assertEquals(1, failureCount.get(), "Exactly one assignment must fail due to driver locking and state validation");

        Driver refreshedDriver = driverRepository.findById(driver.getId()).orElseThrow();
        assertEquals(DriverStatus.BUSY, refreshedDriver.getStatus());
    }

    // ==========================================
    // 5. Concurrency Protection: Vehicle Assignment
    // ==========================================

    @Test
    @DisplayName("Concurrent assignment of the same vehicle to two shipments allows only one to succeed")
    void concurrentAssignment_sameVehicle_onlyOneSucceeds() throws InterruptedException {
        Shipment shipment1 = createTestShipment();
        Shipment shipment2 = createTestShipment();
        Driver driver1 = createTestDriver("d_v_concurrent_1", DriverStatus.AVAILABLE);
        Driver driver2 = createTestDriver("d_v_concurrent_2", DriverStatus.AVAILABLE);
        Vehicle vehicle = createTestVehicle(VehicleStatus.AVAILABLE);

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        executor.submit(() -> {
            try {
                startLatch.await();
                shipmentAssignmentService.assignDriverAndVehicle(shipment1.getId(), driver1.getId(), vehicle.getId());
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                shipmentAssignmentService.assignDriverAndVehicle(shipment2.getId(), driver2.getId(), vehicle.getId());
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished, "Concurrent execution timed out");
        assertEquals(1, successCount.get(), "Exactly one assignment must succeed");
        assertEquals(1, failureCount.get(), "Exactly one assignment must fail due to vehicle locking and state validation");

        Vehicle refreshedVehicle = vehicleRepository.findById(vehicle.getId()).orElseThrow();
        assertEquals(VehicleStatus.IN_USE, refreshedVehicle.getStatus());
    }

    // ==========================================
    // 6. Concurrency Protection: Shipment Lifecycle
    // ==========================================

    @Test
    @DisplayName("Concurrent status transitions on same shipment are serialized by pessimistic lock")
    void concurrentLifecycleTransition_serializedByPessimisticLock() throws InterruptedException {
        Shipment shipment = createTestShipment();
        assertEquals(ShipmentStatus.CREATED, shipment.getStatus());

        // Thread 1 attempts: CREATED -> CONFIRMED (valid)
        // Thread 2 attempts: CREATED -> CANCELLED (valid from CREATED, but invalid once CONFIRMED or vice-versa)
        // Note: From CONFIRMED, CANCELLED is actually valid in ALLOWED_TRANSITIONS:
        // ShipmentStatus.CONFIRMED -> PICKED_UP, CANCELLED.
        // But from CONFIRMED -> IN_TRANSIT is INVALID (must go to PICKED_UP first).
        // If thread 1 transitions to CONFIRMED, thread 2 attempting PICKED_UP vs thread 2 attempting CREATED->PICKED_UP:
        // If Thread 1 is CREATED -> CONFIRMED, and Thread 2 attempts CREATED-based transition that fails from CONFIRMED.
        // Specifically: Thread 2 attempts transition to DELIVERED. (Neither CREATED nor CONFIRMED allows DELIVERED directly).
        // Let's test two competing transitions:
        // Thread 1: CREATED -> CONFIRMED (valid)
        // Thread 2: CREATED -> CONFIRMED (duplicate transition) -> Once Thread 1 commits CONFIRMED, Thread 2 tries CONFIRMED -> CONFIRMED, which throws BadRequestException!

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    shipmentLifecycleService.transitionStatus(shipment.getId(), ShipmentStatus.CONFIRMED);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished, "Concurrent execution timed out");
        // Only one can successfully transition CREATED -> CONFIRMED; the second reads CONFIRMED and fails (cannot transition CONFIRMED -> CONFIRMED)
        assertEquals(1, successCount.get());
        assertEquals(1, failureCount.get());

        Shipment refreshedShipment = shipmentRepository.findById(shipment.getId()).orElseThrow();
        assertEquals(ShipmentStatus.CONFIRMED, refreshedShipment.getStatus());
    }

    // ==========================================
    // 7. Concurrency Protection: Warehouse Movement
    // ==========================================

    @Test
    @DisplayName("Concurrent warehouse movement to the same destination is serialized, preventing duplicate movement")
    void concurrentWarehouseMovement_sameDestination_onlyOneSucceeds() throws InterruptedException {
        Shipment shipment = createTestShipment();
        Warehouse warehouseA = createTestWarehouse("Target WH", WarehouseStatus.ACTIVE);

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    warehouseMovementService.moveShipmentToWarehouse(shipment.getId(), warehouseA.getId(), "Concurrent move");
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished, "Concurrent execution timed out");
        // One thread moves the shipment to warehouseA; the second thread sees currentWarehouse == warehouseA and fails with BadRequestException
        assertEquals(1, successCount.get());
        assertEquals(1, failureCount.get());

        List<WarehouseMovement> movements = warehouseMovementRepository.findByShipmentIdOrderByMovedAtAsc(shipment.getId());
        assertEquals(1, movements.size());
    }
}
