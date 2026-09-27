package com.ahmed.logistics.shipment.service;

import com.ahmed.logistics.exception.BadRequestException;
import com.ahmed.logistics.exception.ResourceNotFoundException;
import com.ahmed.logistics.shipment.dto.ShipmentResponse;
import com.ahmed.logistics.shipment.entity.Shipment;
import com.ahmed.logistics.shipment.entity.ShipmentStatus;
import com.ahmed.logistics.shipment.repository.ShipmentRepository;
import com.ahmed.logistics.shipment.tracking.service.ShipmentTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentLifecycleService {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentTrackingService shipmentTrackingService;

    private static final Map<ShipmentStatus, Set<ShipmentStatus>> ALLOWED_TRANSITIONS = Map.of(
            ShipmentStatus.CREATED, Set.of(ShipmentStatus.CONFIRMED, ShipmentStatus.CANCELLED),
            ShipmentStatus.CONFIRMED, Set.of(ShipmentStatus.PICKED_UP, ShipmentStatus.CANCELLED),
            ShipmentStatus.PICKED_UP, Set.of(ShipmentStatus.IN_TRANSIT),
            ShipmentStatus.IN_TRANSIT, Set.of(ShipmentStatus.OUT_FOR_DELIVERY),
            ShipmentStatus.OUT_FOR_DELIVERY, Set.of(ShipmentStatus.DELIVERED, ShipmentStatus.DELIVERY_FAILED),
            ShipmentStatus.DELIVERY_FAILED, Set.of(ShipmentStatus.RESCHEDULED),
            ShipmentStatus.RESCHEDULED, Set.of(ShipmentStatus.OUT_FOR_DELIVERY),
            ShipmentStatus.DELIVERED, Set.of(ShipmentStatus.RETURNED)
    );

    @Transactional
    public ShipmentResponse transitionStatus(Long shipmentId, ShipmentStatus targetStatus) {
        if (targetStatus == null) {
            throw new BadRequestException("Target shipment status must not be null");
        }

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + shipmentId));

        ShipmentStatus currentStatus = shipment.getStatus();
        log.info("Attempting to transition shipment ID: {} from {} to {}", shipmentId, currentStatus, targetStatus);

        Set<ShipmentStatus> allowedNextStatuses = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Collections.emptySet());

        if (!allowedNextStatuses.contains(targetStatus)) {
            log.warn("Invalid shipment transition requested for ID {}: {} -> {}", shipmentId, currentStatus, targetStatus);
            throw new BadRequestException(
                    String.format("Cannot transition shipment from %s to %s", currentStatus, targetStatus)
            );
        }

        shipment.setStatus(targetStatus);
        Shipment updated = shipmentRepository.save(shipment);
        shipmentTrackingService.recordStatusChange(updated, targetStatus);
        log.info("Shipment ID: {} successfully transitioned to status {}", updated.getId(), updated.getStatus());

        return ShipmentResponse.fromEntity(updated);
    }

    public boolean isTransitionAllowed(ShipmentStatus current, ShipmentStatus target) {
        if (current == null || target == null) {
            return false;
        }
        return ALLOWED_TRANSITIONS.getOrDefault(current, Collections.emptySet()).contains(target);
    }
}
