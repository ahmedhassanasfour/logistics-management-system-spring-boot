package com.ahmed.logistics.shipment.tracking.service;

import com.ahmed.logistics.exception.ResourceNotFoundException;
import com.ahmed.logistics.shipment.dto.ShipmentResponse;
import com.ahmed.logistics.shipment.entity.Shipment;
import com.ahmed.logistics.shipment.entity.ShipmentStatus;
import com.ahmed.logistics.shipment.repository.ShipmentRepository;
import com.ahmed.logistics.shipment.tracking.dto.ShipmentTimelineResponse;
import com.ahmed.logistics.shipment.tracking.dto.ShipmentTrackingResponse;
import com.ahmed.logistics.shipment.tracking.entity.ShipmentTracking;
import com.ahmed.logistics.shipment.tracking.repository.ShipmentTrackingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentTrackingService {

    public static final int MAX_PAGE_SIZE = 100;
    public static final int DEFAULT_PAGE_SIZE = 20;

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

    @Transactional(readOnly = true)
    public Page<ShipmentTrackingResponse> getShipmentTracking(Long shipmentId, Pageable pageable) {
        log.info("Retrieving paginated tracking history for shipment ID: {}", shipmentId);

        if (!shipmentRepository.existsById(shipmentId)) {
            throw new ResourceNotFoundException("Shipment not found with ID: " + shipmentId);
        }

        Pageable safePageable = sanitizePageable(pageable);
        return shipmentTrackingRepository.findByShipmentIdOrderByCreatedAtAsc(shipmentId, safePageable)
                .map(ShipmentTrackingResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public ShipmentTimelineResponse getTimelineByTrackingNumber(String trackingNumber) {
        log.info("Retrieving timeline for tracking number: {}", trackingNumber);

        Shipment shipment = shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with tracking number: " + trackingNumber));

        List<ShipmentTrackingResponse> timeline = shipmentTrackingRepository
                .findByShipmentIdOrderByCreatedAtAsc(shipment.getId())
                .stream()
                .map(ShipmentTrackingResponse::fromEntity)
                .toList();

        return new ShipmentTimelineResponse(
                shipment.getId(),
                shipment.getTrackingNumber(),
                shipment.getStatus(),
                ShipmentResponse.fromEntity(shipment),
                timeline
        );
    }

    public String getDefaultDescription(ShipmentStatus status) {
        if (status == null) {
            return "Status updated";
        }
        return DEFAULT_DESCRIPTIONS.getOrDefault(status, "Status updated to " + status);
    }

    private Pageable sanitizePageable(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return PageRequest.of(0, DEFAULT_PAGE_SIZE, Sort.by(Sort.Direction.ASC, "createdAt"));
        }
        int pageSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        if (pageSize <= 0) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.ASC, "createdAt");
        return PageRequest.of(pageable.getPageNumber(), pageSize, sort);
    }
}
