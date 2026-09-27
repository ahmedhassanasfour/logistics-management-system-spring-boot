package com.ahmed.logistics.shipment.tracking.service;

import com.ahmed.logistics.exception.ResourceNotFoundException;
import com.ahmed.logistics.shipment.entity.Shipment;
import com.ahmed.logistics.shipment.entity.ShipmentStatus;
import com.ahmed.logistics.shipment.repository.ShipmentRepository;
import com.ahmed.logistics.shipment.tracking.dto.ShipmentTrackingResponse;
import com.ahmed.logistics.shipment.tracking.entity.ShipmentTracking;
import com.ahmed.logistics.shipment.tracking.repository.ShipmentTrackingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentTrackingService {

    private final ShipmentTrackingRepository shipmentTrackingRepository;
    private final ShipmentRepository shipmentRepository;

    private static final Map<ShipmentStatus, String> DEFAULT_DESCRIPTIONS = Map.of(
            ShipmentStatus.CREATED, "Shipment created",
            ShipmentStatus.CONFIRMED, "Shipment confirmed",
            ShipmentStatus.PICKED_UP, "Shipment picked up",
            ShipmentStatus.IN_TRANSIT, "Shipment is in transit",
            ShipmentStatus.OUT_FOR_DELIVERY, "Shipment is out for delivery",
            ShipmentStatus.DELIVERED, "Shipment delivered",
            ShipmentStatus.CANCELLED, "Shipment cancelled",
            ShipmentStatus.DELIVERY_FAILED, "Delivery attempt failed",
            ShipmentStatus.RESCHEDULED, "Shipment rescheduled",
            ShipmentStatus.RETURNED, "Shipment returned"
    );

    @Transactional
    public void recordStatusChange(Shipment shipment, ShipmentStatus status, String description, String location) {
        if (shipment == null) {
            throw new IllegalArgumentException("Shipment must not be null when recording tracking status");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status must not be null when recording tracking status");
        }

        String finalDescription = (description != null && !description.isBlank())
                ? description
                : getDefaultDescription(status);

        ShipmentTracking tracking = ShipmentTracking.builder()
                .shipment(shipment)
                .status(status)
                .description(finalDescription)
                .location(location)
                .build();

        shipmentTrackingRepository.save(tracking);
        log.info("Recorded tracking event for shipment ID: {}, status: '{}', description: '{}'",
                shipment.getId(), status, finalDescription);
    }

    @Transactional
    public void recordStatusChange(Shipment shipment, ShipmentStatus status) {
        recordStatusChange(shipment, status, getDefaultDescription(status), null);
    }

    @Transactional(readOnly = true)
    public List<ShipmentTrackingResponse> getShipmentTracking(Long shipmentId) {
        log.info("Retrieving tracking history for shipment ID: {}", shipmentId);

        if (!shipmentRepository.existsById(shipmentId)) {
            throw new ResourceNotFoundException("Shipment not found with ID: " + shipmentId);
        }

        return shipmentTrackingRepository.findByShipmentIdOrderByCreatedAtAsc(shipmentId)
                .stream()
                .map(ShipmentTrackingResponse::fromEntity)
                .toList();
    }

    public String getDefaultDescription(ShipmentStatus status) {
        if (status == null) {
            return "Status updated";
        }
        return DEFAULT_DESCRIPTIONS.getOrDefault(status, "Status updated to " + status);
    }
}
