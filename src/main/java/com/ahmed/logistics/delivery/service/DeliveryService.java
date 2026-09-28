package com.ahmed.logistics.delivery.service;

import com.ahmed.logistics.delivery.dto.DeliveryResponse;
import com.ahmed.logistics.delivery.entity.Delivery;
import com.ahmed.logistics.delivery.entity.DeliveryStatus;
import com.ahmed.logistics.delivery.repository.DeliveryRepository;
import com.ahmed.logistics.driver.entity.Driver;
import com.ahmed.logistics.driver.entity.DriverStatus;
import com.ahmed.logistics.driver.repository.DriverRepository;
import com.ahmed.logistics.exception.BadRequestException;
import com.ahmed.logistics.exception.ResourceNotFoundException;
import com.ahmed.logistics.shipment.entity.Shipment;
import com.ahmed.logistics.shipment.entity.ShipmentStatus;
import com.ahmed.logistics.shipment.repository.ShipmentRepository;
import com.ahmed.logistics.shipment.service.ShipmentLifecycleService;
import com.ahmed.logistics.shipment.tracking.service.ShipmentTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.ahmed.logistics.delivery.pod.repository.ProofOfDeliveryRepository;
import com.ahmed.logistics.delivery.reschedule.entity.RescheduleStatus;
import com.ahmed.logistics.delivery.reschedule.repository.DeliveryRescheduleRepository;
import com.ahmed.logistics.vehicle.entity.VehicleStatus;
import com.ahmed.logistics.vehicle.repository.VehicleRepository;
import com.ahmed.logistics.config.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final ShipmentRepository shipmentRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final ShipmentLifecycleService shipmentLifecycleService;
    private final ShipmentTrackingService shipmentTrackingService;
    private final ProofOfDeliveryRepository proofOfDeliveryRepository;
    private final DeliveryRescheduleRepository deliveryRescheduleRepository;

    private static final String TRACKING_DELIVERY_STARTED = "Delivery started";
    private static final String TRACKING_DELIVERY_COMPLETED = "Delivery completed";

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.DRIVER, allEntries = true),
            @CacheEvict(value = CacheNames.VEHICLE, allEntries = true)
    })
    public DeliveryResponse startDelivery(Long shipmentId) {
        log.info("Starting delivery for shipment ID: {}", shipmentId);

        // 1. Lock the shipment using existing findByIdForUpdate
        Shipment shipment = shipmentRepository.findByIdForUpdate(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + shipmentId));

        // 2 & 3. Verify shipment has an assigned driver
        if (shipment.getDriver() == null) {
            log.warn("Cannot start delivery: Shipment ID {} does not have an assigned driver", shipmentId);
            throw new BadRequestException("Shipment does not have an assigned driver");
        }

        // 4. Verify shipment has an assigned vehicle
        if (shipment.getVehicle() == null) {
            log.warn("Cannot start delivery: Shipment ID {} does not have an assigned vehicle", shipmentId);
            throw new BadRequestException("Shipment does not have an assigned vehicle");
        }

        // 5. Shipment must currently be OUT_FOR_DELIVERY
        if (shipment.getStatus() != ShipmentStatus.OUT_FOR_DELIVERY) {
            log.warn("Cannot start delivery: Shipment ID {} is in status {}, expected OUT_FOR_DELIVERY",
                    shipmentId, shipment.getStatus());
            throw new BadRequestException(
                    String.format("Shipment must be in OUT_FOR_DELIVERY status to start delivery. Current status: %s",
                            shipment.getStatus())
            );
        }

        // 6. Check existing active Delivery for the shipment
        Optional<Delivery> activeDeliveryOpt = deliveryRepository.findActiveDeliveryForUpdate(
                shipmentId, Set.of(DeliveryStatus.ASSIGNED, DeliveryStatus.IN_PROGRESS)
        );

        Delivery delivery;
        if (activeDeliveryOpt.isPresent()) {
            Delivery existing = activeDeliveryOpt.get();
            if (existing.getStatus() == DeliveryStatus.IN_PROGRESS) {
                log.warn("Cannot start delivery: Delivery ID {} is already IN_PROGRESS", existing.getId());
                throw new BadRequestException("Shipment already has an active delivery in progress");
            }
            // Transition ASSIGNED -> IN_PROGRESS
            existing.setStatus(DeliveryStatus.IN_PROGRESS);
            existing.setStartedAt(LocalDateTime.now());
            if (existing.getDriver() == null && shipment.getDriver() != null) {
                existing.setDriver(shipment.getDriver());
            }
            delivery = existing;
        } else {
            delivery = Delivery.builder()
                    .shipment(shipment)
                    .driver(shipment.getDriver())
                    .status(DeliveryStatus.IN_PROGRESS)
                    .startedAt(LocalDateTime.now())
                    .build();
        }

        // 8. Persist the Delivery
        Delivery savedDelivery = deliveryRepository.save(delivery);

        // Driver should remain BUSY
        Driver driver = shipment.getDriver();
        if (driver.getStatus() != DriverStatus.BUSY) {
            driver.setStatus(DriverStatus.BUSY);
            driverRepository.save(driver);
        }

        // 9. Record shipment tracking using existing ShipmentTrackingService
        String location = buildDeliveryLocation(shipment);
        shipmentTrackingService.recordStatusChange(
                shipment,
                shipment.getStatus(),
                TRACKING_DELIVERY_STARTED,
                location
        );

        log.info("Delivery ID: {} started successfully for shipment ID: {}", savedDelivery.getId(), shipmentId);
        return DeliveryResponse.fromEntity(savedDelivery);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.DRIVER, allEntries = true),
            @CacheEvict(value = CacheNames.VEHICLE, allEntries = true)
    })
    public DeliveryResponse completeDelivery(Long shipmentId, String deliveryNotes) {
        log.info("Completing delivery for shipment ID: {}", shipmentId);

        // 1. Lock the shipment
        Shipment shipment = shipmentRepository.findByIdForUpdate(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + shipmentId));

        // 2. Find and lock its Delivery
        Delivery delivery = deliveryRepository.findActiveDeliveryForUpdate(shipmentId, Set.of(DeliveryStatus.IN_PROGRESS))
                .or(() -> deliveryRepository.findByShipmentIdForUpdate(shipmentId))
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found for shipment ID: " + shipmentId));

        // 3. Lock the assigned Driver
        Driver driver = null;
        if (delivery.getDriver() != null) {
            driver = driverRepository.findByIdForUpdate(delivery.getDriver().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Driver not found with ID: " + delivery.getDriver().getId()));
        }

        // 4. Verify Delivery is IN_PROGRESS
        if (delivery.getStatus() != DeliveryStatus.IN_PROGRESS) {
            log.warn("Cannot complete delivery: Delivery ID {} is in status {}, expected IN_PROGRESS",
                    delivery.getId(), delivery.getStatus());
            throw new BadRequestException(
                    String.format("Delivery must be IN_PROGRESS to be completed. Current status: %s",
                            delivery.getStatus())
            );
        }

        // 5. Verify POD exists
        if (!proofOfDeliveryRepository.existsByDeliveryId(delivery.getId())) {
            log.warn("Cannot complete delivery: Proof of delivery does not exist for delivery ID {}", delivery.getId());
            throw new BadRequestException("Proof of delivery is required before completing the delivery");
        }

        // 6. Set Delivery: status = DELIVERED, deliveredAt = now, deliveryNotes
        delivery.setStatus(DeliveryStatus.DELIVERED);
        delivery.setDeliveredAt(LocalDateTime.now());
        delivery.setDeliveryNotes(deliveryNotes);
        Delivery savedDelivery = deliveryRepository.save(delivery);

        // 7 & 8. Transition Shipment through existing ShipmentLifecycleService and record tracking
        String location = buildDeliveryLocation(shipment);
        shipmentLifecycleService.applyStatusTransition(
                shipment,
                ShipmentStatus.DELIVERED,
                TRACKING_DELIVERY_COMPLETED,
                location
        );

        // 9. Set Driver: BUSY -> AVAILABLE
        if (driver != null) {
            driver.setStatus(DriverStatus.AVAILABLE);
            driverRepository.save(driver);
            log.info("Driver ID: {} status transitioned to AVAILABLE", driver.getId());
        }

        // 10. Set Vehicle: IN_USE -> AVAILABLE
        if (shipment.getVehicle() != null) {
            vehicleRepository.findByIdForUpdate(shipment.getVehicle().getId())
                    .ifPresent(vehicle -> {
                        vehicle.setStatus(VehicleStatus.AVAILABLE);
                        vehicleRepository.save(vehicle);
                        log.info("Vehicle ID: {} status transitioned to AVAILABLE", vehicle.getId());
                    });
        }

        // 11. Mark active reschedule as COMPLETED if present
        deliveryRescheduleRepository.findFirstByShipmentIdAndStatus(shipmentId, RescheduleStatus.SCHEDULED)
                .ifPresent(reschedule -> {
                    reschedule.setStatus(RescheduleStatus.COMPLETED);
                    deliveryRescheduleRepository.save(reschedule);
                    log.info("Reschedule ID: {} marked as COMPLETED for shipment ID: {}", reschedule.getId(), shipmentId);
                });

        log.info("Delivery ID: {} successfully completed for shipment ID: {}", savedDelivery.getId(), shipmentId);
        return DeliveryResponse.fromEntity(savedDelivery);
    }

    @Transactional(readOnly = true)
    public DeliveryResponse getDelivery(Long deliveryId) {
        log.info("Fetching delivery with ID: {}", deliveryId);
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found with ID: " + deliveryId));
        return DeliveryResponse.fromEntity(delivery);
    }

    @Transactional(readOnly = true)
    public DeliveryResponse getDeliveryByShipment(Long shipmentId) {
        log.info("Fetching delivery for shipment ID: {}", shipmentId);
        if (!shipmentRepository.existsById(shipmentId)) {
            throw new ResourceNotFoundException("Shipment not found with ID: " + shipmentId);
        }
        Delivery delivery = deliveryRepository.findTopByShipmentIdOrderByCreatedAtDesc(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found for shipment ID: " + shipmentId));
        return DeliveryResponse.fromEntity(delivery);
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
