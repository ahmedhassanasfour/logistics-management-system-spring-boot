package com.ahmed.logistics.delivery.failure.service;

import com.ahmed.logistics.delivery.entity.Delivery;
import com.ahmed.logistics.delivery.entity.DeliveryStatus;
import com.ahmed.logistics.delivery.failure.dto.CreateDeliveryFailureRequest;
import com.ahmed.logistics.delivery.failure.dto.DeliveryFailureResponse;
import com.ahmed.logistics.delivery.failure.entity.DeliveryFailure;
import com.ahmed.logistics.delivery.failure.repository.DeliveryFailureRepository;
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
import com.ahmed.logistics.vehicle.entity.Vehicle;
import com.ahmed.logistics.vehicle.entity.VehicleStatus;
import com.ahmed.logistics.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.ahmed.logistics.config.CacheNames;
import com.ahmed.logistics.event.DeliveryFailedEvent;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryFailureService {

    private final DeliveryFailureRepository deliveryFailureRepository;
    private final DeliveryRepository deliveryRepository;
    private final ShipmentRepository shipmentRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final ShipmentLifecycleService shipmentLifecycleService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.DRIVER, allEntries = true),
            @CacheEvict(value = CacheNames.VEHICLE, allEntries = true)
    })
    public DeliveryFailureResponse failDelivery(Long deliveryId, CreateDeliveryFailureRequest request) {
        log.info("Recording delivery failure for delivery ID: {}, reason: {}", deliveryId, request.reason());

        // Step 1: Find and lock Delivery
        Delivery delivery = deliveryRepository.findByIdForUpdate(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found with ID: " + deliveryId));

        // Step 2: Validate Delivery status is IN_PROGRESS
        if (delivery.getStatus() != DeliveryStatus.IN_PROGRESS) {
            log.warn("Cannot fail delivery: Delivery ID {} is in status {}, expected IN_PROGRESS",
                    deliveryId, delivery.getStatus());
            throw new BadRequestException(
                    String.format("Delivery cannot be failed because it is not in progress. Current status: %s",
                            delivery.getStatus())
            );
        }

        // Step 3: Validate and lock Shipment
        Shipment shipment = delivery.getShipment();
        if (shipment == null) {
            log.error("Delivery ID {} is missing associated shipment", deliveryId);
            throw new BadRequestException("Delivery is not associated with a valid shipment");
        }

        Shipment lockedShipment = shipmentRepository.findByIdForUpdate(shipment.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + shipment.getId()));

        // Step 4: Create Failure Record
        DeliveryFailure failure = DeliveryFailure.builder()
                .delivery(delivery)
                .reason(request.reason())
                .notes(request.notes() != null ? request.notes().trim() : null)
                .failedAt(LocalDateTime.now())
                .build();

        DeliveryFailure savedFailure = deliveryFailureRepository.save(failure);

        // Step 5: Update Delivery status to FAILED
        delivery.setStatus(DeliveryStatus.FAILED);
        deliveryRepository.save(delivery);

        // Step 6 & 8: Update Shipment through existing ShipmentLifecycleService and record tracking
        String trackingDescription = "Delivery failed: " + request.reason();
        String location = buildDeliveryLocation(lockedShipment);
        shipmentLifecycleService.applyStatusTransition(
                lockedShipment,
                ShipmentStatus.DELIVERY_FAILED,
                trackingDescription,
                location
        );

        // Step 7: Driver & Vehicle State - Lock assigned Driver and Vehicle, change to AVAILABLE
        if (delivery.getDriver() != null) {
            Driver driver = driverRepository.findByIdForUpdate(delivery.getDriver().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Driver not found with ID: " + delivery.getDriver().getId()));
            driver.setStatus(DriverStatus.AVAILABLE);
            driverRepository.save(driver);
            log.info("Driver ID: {} status transitioned to AVAILABLE after delivery failure", driver.getId());
        }

        if (lockedShipment.getVehicle() != null) {
            Vehicle vehicle = vehicleRepository.findByIdForUpdate(lockedShipment.getVehicle().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with ID: " + lockedShipment.getVehicle().getId()));
            vehicle.setStatus(VehicleStatus.AVAILABLE);
            vehicleRepository.save(vehicle);
            log.info("Vehicle ID: {} status transitioned to AVAILABLE after delivery failure", vehicle.getId());
        }

        log.info("Delivery failure recorded successfully with ID: {} for delivery ID: {}", savedFailure.getId(), deliveryId);

        Long customerId = lockedShipment.getCustomer() != null ? lockedShipment.getCustomer().getId() : null;
        Long driverId = delivery.getDriver() != null ? delivery.getDriver().getId() : null;
        eventPublisher.publishEvent(new DeliveryFailedEvent(delivery.getId(), lockedShipment.getId(), customerId, driverId, request.reason()));

        return DeliveryFailureResponse.fromEntity(savedFailure);
    }

    @Transactional(readOnly = true)
    public List<DeliveryFailureResponse> getFailuresByDelivery(Long deliveryId) {
        log.info("Fetching delivery failures for delivery ID: {}", deliveryId);

        if (!deliveryRepository.existsById(deliveryId)) {
            throw new ResourceNotFoundException("Delivery not found with ID: " + deliveryId);
        }

        return deliveryFailureRepository.findByDeliveryIdOrderByFailedAtDesc(deliveryId)
                .stream()
                .map(DeliveryFailureResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DeliveryFailureResponse> getFailuresByShipment(Long shipmentId) {
        log.info("Fetching delivery failures for shipment ID: {}", shipmentId);

        if (!shipmentRepository.existsById(shipmentId)) {
            throw new ResourceNotFoundException("Shipment not found with ID: " + shipmentId);
        }

        return deliveryFailureRepository.findByShipmentIdOrderByFailedAtDesc(shipmentId)
                .stream()
                .map(DeliveryFailureResponse::fromEntity)
                .toList();
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
