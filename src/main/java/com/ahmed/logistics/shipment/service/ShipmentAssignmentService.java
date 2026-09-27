package com.ahmed.logistics.shipment.service;

import com.ahmed.logistics.driver.entity.Driver;
import com.ahmed.logistics.driver.entity.DriverStatus;
import com.ahmed.logistics.driver.repository.DriverRepository;
import com.ahmed.logistics.exception.BadRequestException;
import com.ahmed.logistics.exception.ResourceNotFoundException;
import com.ahmed.logistics.shipment.dto.ShipmentResponse;
import com.ahmed.logistics.shipment.entity.Shipment;
import com.ahmed.logistics.shipment.entity.ShipmentStatus;
import com.ahmed.logistics.shipment.repository.ShipmentRepository;
import com.ahmed.logistics.shipment.tracking.service.ShipmentTrackingService;
import com.ahmed.logistics.user.entity.Role;
import com.ahmed.logistics.vehicle.entity.Vehicle;
import com.ahmed.logistics.vehicle.entity.VehicleStatus;
import com.ahmed.logistics.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentAssignmentService {

    private final ShipmentRepository shipmentRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final ShipmentTrackingService shipmentTrackingService;

    public static final Set<ShipmentStatus> ASSIGNABLE_SHIPMENT_STATUSES = Set.of(
            ShipmentStatus.CREATED,
            ShipmentStatus.CONFIRMED
    );

    public static final Set<ShipmentStatus> ACTIVE_SHIPMENT_STATUSES = Set.of(
            ShipmentStatus.CREATED,
            ShipmentStatus.CONFIRMED,
            ShipmentStatus.PICKED_UP,
            ShipmentStatus.IN_TRANSIT,
            ShipmentStatus.OUT_FOR_DELIVERY,
            ShipmentStatus.DELIVERY_FAILED,
            ShipmentStatus.RESCHEDULED
    );

    public static final String ASSIGNMENT_TRACKING_DESCRIPTION = "Driver and vehicle assigned";

    @Transactional
    public ShipmentResponse assignDriverAndVehicle(Long shipmentId, Long driverId, Long vehicleId) {
        if (shipmentId == null) {
            throw new BadRequestException("Shipment ID must not be null");
        }
        if (driverId == null) {
            throw new BadRequestException("Driver ID must not be null");
        }
        if (vehicleId == null) {
            throw new BadRequestException("Vehicle ID must not be null");
        }

        log.info("Attempting to assign driver ID: {} and vehicle ID: {} to shipment ID: {}",
                driverId, vehicleId, shipmentId);

        // 1. Lock Shipment
        Shipment shipment = shipmentRepository.findByIdForUpdate(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + shipmentId));

        // 2. Validate Shipment Status
        if (!ASSIGNABLE_SHIPMENT_STATUSES.contains(shipment.getStatus())) {
            log.warn("Cannot assign driver/vehicle to shipment ID: {} in status: {}", shipmentId, shipment.getStatus());
            throw new BadRequestException("Shipment cannot be assigned in status: " + shipment.getStatus());
        }

        // 3. Validate No Existing Assignment on this Shipment
        if (shipment.getDriver() != null || shipment.getVehicle() != null) {
            log.warn("Shipment ID: {} already has an assigned driver or vehicle", shipmentId);
            throw new BadRequestException("Shipment already has an assigned driver or vehicle");
        }

        // 4. Lock Driver Row & Validate
        Driver driver = driverRepository.findByIdForUpdate(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with ID: " + driverId));

        if (driver.getUser() == null || driver.getUser().getRole() != Role.DRIVER) {
            log.warn("User associated with driver ID: {} does not have DRIVER role", driverId);
            throw new BadRequestException("User associated with driver is not a valid driver");
        }

        if (driver.getStatus() != DriverStatus.AVAILABLE) {
            log.warn("Driver ID: {} is not AVAILABLE. Current status: {}", driverId, driver.getStatus());
            throw new BadRequestException("Driver is not available for assignment. Current status: " + driver.getStatus());
        }

        // 5. Lock Vehicle Row & Validate
        Vehicle vehicle = vehicleRepository.findByIdForUpdate(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with ID: " + vehicleId));

        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            log.warn("Vehicle ID: {} is not AVAILABLE. Current status: {}", vehicleId, vehicle.getStatus());
            throw new BadRequestException("Vehicle is not available for assignment. Current status: " + vehicle.getStatus());
        }

        // 6. Check Active Assignment for Driver
        if (shipmentRepository.existsByDriverIdAndStatusIn(driverId, ACTIVE_SHIPMENT_STATUSES)) {
            log.warn("Driver ID: {} is already assigned to another active shipment", driverId);
            throw new BadRequestException("Driver is already assigned to an active shipment");
        }

        // 7. Check Active Assignment for Vehicle
        if (shipmentRepository.existsByVehicleIdAndStatusIn(vehicleId, ACTIVE_SHIPMENT_STATUSES)) {
            log.warn("Vehicle ID: {} is already assigned to another active shipment", vehicleId);
            throw new BadRequestException("Vehicle is already assigned to an active shipment");
        }

        // 8. Assign Driver & Vehicle
        driver.setStatus(DriverStatus.BUSY);
        vehicle.setStatus(VehicleStatus.IN_USE);
        shipment.setDriver(driver);
        shipment.setVehicle(vehicle);

        driverRepository.save(driver);
        vehicleRepository.save(vehicle);
        Shipment savedShipment = shipmentRepository.save(shipment);

        // 9. Tracking Event in Same Transaction
        shipmentTrackingService.recordStatusChange(
                savedShipment,
                savedShipment.getStatus(),
                ASSIGNMENT_TRACKING_DESCRIPTION,
                null
        );

        log.info("Shipment ID: {} successfully assigned Driver ID: {} and Vehicle ID: {}",
                savedShipment.getId(), driver.getId(), vehicle.getId());

        return ShipmentResponse.fromEntity(savedShipment);
    }
}
