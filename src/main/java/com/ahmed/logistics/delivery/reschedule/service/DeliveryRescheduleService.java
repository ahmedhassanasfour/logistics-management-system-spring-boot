package com.ahmed.logistics.delivery.reschedule.service;

import com.ahmed.logistics.delivery.dto.DeliveryResponse;
import com.ahmed.logistics.delivery.entity.Delivery;
import com.ahmed.logistics.delivery.entity.DeliveryStatus;
import com.ahmed.logistics.delivery.repository.DeliveryRepository;
import com.ahmed.logistics.delivery.reschedule.dto.CreateRescheduleRequest;
import com.ahmed.logistics.delivery.reschedule.dto.RescheduleResponse;
import com.ahmed.logistics.delivery.reschedule.entity.DeliveryReschedule;
import com.ahmed.logistics.delivery.reschedule.entity.RescheduleStatus;
import com.ahmed.logistics.delivery.reschedule.repository.DeliveryRescheduleRepository;
import com.ahmed.logistics.driver.entity.Driver;
import com.ahmed.logistics.driver.entity.DriverStatus;
import com.ahmed.logistics.driver.repository.DriverRepository;
import com.ahmed.logistics.exception.BadRequestException;
import com.ahmed.logistics.exception.ResourceNotFoundException;
import com.ahmed.logistics.shipment.entity.Shipment;
import com.ahmed.logistics.shipment.entity.ShipmentStatus;
import com.ahmed.logistics.shipment.repository.ShipmentRepository;
import com.ahmed.logistics.shipment.service.ShipmentLifecycleService;
import com.ahmed.logistics.vehicle.entity.Vehicle;
import com.ahmed.logistics.vehicle.entity.VehicleStatus;
import com.ahmed.logistics.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryRescheduleService {

    private final DeliveryRescheduleRepository deliveryRescheduleRepository;
    private final DeliveryRepository deliveryRepository;
    private final ShipmentRepository shipmentRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final ShipmentLifecycleService shipmentLifecycleService;

    private static final String TRACKING_DELIVERY_RESCHEDULED = "Delivery rescheduled";
    private static final String TRACKING_NEW_ATTEMPT_CREATED = "New delivery attempt created";

    @Transactional
    public RescheduleResponse reschedule(Long deliveryId, CreateRescheduleRequest request) {
        log.info("Attempting to reschedule delivery ID: {}", deliveryId);

        if (deliveryId == null) {
            throw new BadRequestException("Delivery ID must not be null");
        }
        if (request == null) {
            throw new BadRequestException("Reschedule request must not be null");
        }

        // Validate scheduledAt strictly in the future using server-side time
        if (request.scheduledAt() == null || !request.scheduledAt().isAfter(LocalDateTime.now())) {
            log.warn("Cannot reschedule delivery ID {}: scheduledAt {} is not in the future",
                    deliveryId, request.scheduledAt());
            throw new BadRequestException("Scheduled time must be strictly in the future");
        }

        // Preliminary check for delivery existence to retrieve associated shipment
        Delivery deliveryLookup = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found with ID: " + deliveryId));

        if (deliveryLookup.getShipment() == null) {
            log.error("Delivery ID {} is missing associated shipment", deliveryId);
            throw new BadRequestException("Delivery is not associated with a valid shipment");
        }

        Long shipmentId = deliveryLookup.getShipment().getId();

        // Step 1: Lock Shipment using ShipmentRepository.findByIdForUpdate
        Shipment lockedShipment = shipmentRepository.findByIdForUpdate(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + shipmentId));

        // Step 2: Lock Delivery using DeliveryRepository.findByIdForUpdate
        Delivery lockedDelivery = deliveryRepository.findByIdForUpdate(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found with ID: " + deliveryId));

        // Step 3: Validate Delivery status is FAILED
        if (lockedDelivery.getStatus() != DeliveryStatus.FAILED) {
            log.warn("Cannot reschedule delivery ID {}: delivery status is {}, expected FAILED",
                    deliveryId, lockedDelivery.getStatus());
            throw new BadRequestException(
                    String.format("Delivery must be in FAILED status to be rescheduled. Current status: %s",
                            lockedDelivery.getStatus())
            );
        }

        // Step 4: Validate Shipment status is DELIVERY_FAILED
        if (lockedShipment.getStatus() != ShipmentStatus.DELIVERY_FAILED) {
            log.warn("Cannot reschedule delivery ID {}: shipment ID {} status is {}, expected DELIVERY_FAILED",
                    deliveryId, shipmentId, lockedShipment.getStatus());
            throw new BadRequestException(
                    String.format("Shipment must be in DELIVERY_FAILED status to be rescheduled. Current status: %s",
                            lockedShipment.getStatus())
            );
        }

        // Step 5: Prevent Duplicate Reschedule for the same failed Delivery
        if (deliveryRescheduleRepository.existsByFailedDeliveryId(deliveryId)) {
            log.warn("Failed delivery ID {} has already been rescheduled", deliveryId);
            throw new BadRequestException("Delivery attempt has already been rescheduled");
        }

        // Step 7: Create Reschedule Record
        DeliveryReschedule reschedule = DeliveryReschedule.builder()
                .shipment(lockedShipment)
                .failedDelivery(lockedDelivery)
                .scheduledAt(request.scheduledAt())
                .reason(request.reason() != null ? request.reason().trim() : null)
                .notes(request.notes() != null ? request.notes().trim() : null)
                .status(RescheduleStatus.SCHEDULED)
                .build();

        DeliveryReschedule savedReschedule = deliveryRescheduleRepository.save(reschedule);

        // Step 8 & 9: Shipment Lifecycle (DELIVERY_FAILED -> RESCHEDULED) & Tracking
        String location = buildDeliveryLocation(lockedShipment);
        shipmentLifecycleService.applyStatusTransition(
                lockedShipment,
                ShipmentStatus.RESCHEDULED,
                TRACKING_DELIVERY_RESCHEDULED,
                location
        );

        log.info("Delivery ID: {} rescheduled successfully. Reschedule ID: {}", deliveryId, savedReschedule.getId());
        return RescheduleResponse.fromEntity(savedReschedule);
    }

    @Transactional
    public DeliveryResponse createNewDeliveryAttempt(Long shipmentId) {
        log.info("Creating new delivery attempt for shipment ID: {}", shipmentId);

        if (shipmentId == null) {
            throw new BadRequestException("Shipment ID must not be null");
        }

        // 1. Lock Shipment
        Shipment shipment = shipmentRepository.findByIdForUpdate(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + shipmentId));

        // 2. Verify Shipment status is RESCHEDULED
        if (shipment.getStatus() != ShipmentStatus.RESCHEDULED) {
            log.warn("Cannot create new delivery attempt: Shipment ID {} status is {}, expected RESCHEDULED",
                    shipmentId, shipment.getStatus());
            throw new BadRequestException(
                    String.format("Shipment must be in RESCHEDULED status to create a new delivery attempt. Current status: %s",
                            shipment.getStatus())
            );
        }

        // 3. Verify there is no active Delivery attempt
        if (deliveryRepository.existsByShipmentIdAndStatusIn(shipmentId, Set.of(DeliveryStatus.ASSIGNED, DeliveryStatus.IN_PROGRESS))) {
            log.warn("Cannot create new delivery attempt: Shipment ID {} already has an active delivery attempt", shipmentId);
            throw new BadRequestException("Shipment already has an active delivery attempt in progress or assigned");
        }

        // 4. Verify an appropriate driver/vehicle assignment exists or is handled
        if (shipment.getDriver() == null) {
            log.warn("Cannot create new delivery attempt: Shipment ID {} does not have an assigned driver", shipmentId);
            throw new BadRequestException("Shipment must have an assigned driver to create a new delivery attempt");
        }

        Driver driver = driverRepository.findByIdForUpdate(shipment.getDriver().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with ID: " + shipment.getDriver().getId()));

        if (driver.getStatus() != DriverStatus.AVAILABLE) {
            log.warn("Cannot create new delivery attempt: Driver ID {} is not AVAILABLE. Current status: {}",
                    driver.getId(), driver.getStatus());
            throw new BadRequestException("Assigned driver is not available for assignment. Current status: " + driver.getStatus());
        }

        if (shipment.getVehicle() != null) {
            Vehicle vehicle = vehicleRepository.findByIdForUpdate(shipment.getVehicle().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with ID: " + shipment.getVehicle().getId()));

            if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
                log.warn("Cannot create new delivery attempt: Vehicle ID {} is not AVAILABLE. Current status: {}",
                        vehicle.getId(), vehicle.getStatus());
                throw new BadRequestException("Assigned vehicle is not available for assignment. Current status: " + vehicle.getStatus());
            }
            vehicle.setStatus(VehicleStatus.IN_USE);
            vehicleRepository.save(vehicle);
        }

        driver.setStatus(DriverStatus.BUSY);
        driverRepository.save(driver);

        // 5 & 6. Create a NEW Delivery record with status = ASSIGNED
        Delivery newDelivery = Delivery.builder()
                .shipment(shipment)
                .driver(driver)
                .status(DeliveryStatus.ASSIGNED)
                .build();

        Delivery savedDelivery = deliveryRepository.save(newDelivery);

        // 7 & 8. Update Shipment lifecycle (RESCHEDULED -> OUT_FOR_DELIVERY) and record tracking
        String location = buildDeliveryLocation(shipment);
        shipmentLifecycleService.applyStatusTransition(
                shipment,
                ShipmentStatus.OUT_FOR_DELIVERY,
                TRACKING_NEW_ATTEMPT_CREATED,
                location
        );

        log.info("New delivery attempt ID: {} created successfully for shipment ID: {}", savedDelivery.getId(), shipmentId);
        return DeliveryResponse.fromEntity(savedDelivery);
    }

    @Transactional(readOnly = true)
    public List<RescheduleResponse> getReschedulesByShipment(Long shipmentId) {
        log.info("Fetching reschedules for shipment ID: {}", shipmentId);

        if (shipmentId == null) {
            throw new BadRequestException("Shipment ID must not be null");
        }

        if (!shipmentRepository.existsById(shipmentId)) {
            throw new ResourceNotFoundException("Shipment not found with ID: " + shipmentId);
        }

        return deliveryRescheduleRepository.findByShipmentIdOrderByScheduledAtDesc(shipmentId)
                .stream()
                .map(RescheduleResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public RescheduleResponse getRescheduleById(Long id) {
        log.info("Fetching reschedule with ID: {}", id);

        if (id == null) {
            throw new BadRequestException("Reschedule ID must not be null");
        }

        DeliveryReschedule reschedule = deliveryRescheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery reschedule not found with ID: " + id));

        return RescheduleResponse.fromEntity(reschedule);
    }

    @Transactional
    public RescheduleResponse markRescheduleCompleted(Long rescheduleId) {
        log.info("Marking reschedule ID: {} as COMPLETED", rescheduleId);

        if (rescheduleId == null) {
            throw new BadRequestException("Reschedule ID must not be null");
        }

        DeliveryReschedule reschedule = deliveryRescheduleRepository.findByIdForUpdate(rescheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery reschedule not found with ID: " + rescheduleId));

        reschedule.setStatus(RescheduleStatus.COMPLETED);
        DeliveryReschedule updated = deliveryRescheduleRepository.save(reschedule);

        return RescheduleResponse.fromEntity(updated);
    }

    @Transactional
    public void markRescheduleCompletedForShipment(Long shipmentId) {
        if (shipmentId == null) {
            return;
        }

        deliveryRescheduleRepository.findFirstByShipmentIdAndStatus(shipmentId, RescheduleStatus.SCHEDULED)
                .ifPresent(reschedule -> {
                    reschedule.setStatus(RescheduleStatus.COMPLETED);
                    deliveryRescheduleRepository.save(reschedule);
                    log.info("Reschedule ID: {} marked as COMPLETED for shipment ID: {}", reschedule.getId(), shipmentId);
                });
    }

    private String buildDeliveryLocation(Shipment shipment) {
        if (shipment == null) {
            return null;
        }
        String address = shipment.getDeliveryAddress();
        String city = shipment.getDeliveryCity();
        if (address != null && !address.isBlank() && city != null && !city.isBlank()) {
            return address + ", " + city;
        } else if (city != null && !city.isBlank()) {
            return city;
        } else if (address != null && !address.isBlank()) {
            return address;
        }
        return null;
    }
}
