package com.ahmed.logistics.delivery.pod.service;

import com.ahmed.logistics.delivery.entity.Delivery;
import com.ahmed.logistics.delivery.entity.DeliveryStatus;
import com.ahmed.logistics.delivery.pod.dto.CreateProofOfDeliveryRequest;
import com.ahmed.logistics.delivery.pod.dto.ProofOfDeliveryResponse;
import com.ahmed.logistics.delivery.pod.entity.ProofOfDelivery;
import com.ahmed.logistics.delivery.pod.repository.ProofOfDeliveryRepository;
import com.ahmed.logistics.delivery.repository.DeliveryRepository;
import com.ahmed.logistics.exception.BadRequestException;
import com.ahmed.logistics.exception.ResourceNotFoundException;
import com.ahmed.logistics.shipment.entity.Shipment;
import com.ahmed.logistics.shipment.repository.ShipmentRepository;
import com.ahmed.logistics.shipment.tracking.service.ShipmentTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProofOfDeliveryService {

    private final ProofOfDeliveryRepository proofOfDeliveryRepository;
    private final DeliveryRepository deliveryRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentTrackingService shipmentTrackingService;

    private static final String TRACKING_POD_RECORDED = "Proof of delivery recorded";

    @Transactional
    public ProofOfDeliveryResponse createProofOfDelivery(Long deliveryId, CreateProofOfDeliveryRequest request) {
        log.info("Creating proof of delivery for delivery ID: {}", deliveryId);

        // 1. Lock Delivery using pessimistic locking
        Delivery delivery = deliveryRepository.findByIdForUpdate(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found with ID: " + deliveryId));

        // 2 & 3. Verify Delivery status is IN_PROGRESS
        if (delivery.getStatus() != DeliveryStatus.IN_PROGRESS) {
            log.warn("Cannot create proof of delivery: Delivery ID {} has status {}, expected IN_PROGRESS",
                    deliveryId, delivery.getStatus());
            throw new BadRequestException(
                    String.format("Cannot create proof of delivery: Delivery is not in progress. Current status: %s",
                            delivery.getStatus())
            );
        }

        // 4. Verify no POD already exists
        if (proofOfDeliveryRepository.existsByDeliveryId(deliveryId)) {
            log.warn("Proof of delivery already exists for delivery ID: {}", deliveryId);
            throw new BadRequestException("Proof of delivery already exists for delivery ID: " + deliveryId);
        }

        // 5. Verify the shipment associated with the Delivery is valid
        Shipment shipment = delivery.getShipment();
        if (shipment == null) {
            log.error("Delivery ID {} is missing associated shipment", deliveryId);
            throw new BadRequestException("Delivery is not associated with a valid shipment");
        }

        // 6 & 7. Create the POD with recipient details and confirmedAt
        ProofOfDelivery pod = ProofOfDelivery.builder()
                .delivery(delivery)
                .recipientName(request.recipientName().trim())
                .recipientPhone(request.recipientPhone().trim())
                .recipientId(request.recipientId() != null ? request.recipientId().trim() : null)
                .notes(request.notes() != null ? request.notes().trim() : null)
                .confirmedAt(LocalDateTime.now())
                .build();

        // 8. Persist the POD
        ProofOfDelivery savedPod = proofOfDeliveryRepository.save(pod);

        // 9. Record shipment tracking without changing shipment status
        String location = buildDeliveryLocation(shipment);
        shipmentTrackingService.recordStatusChange(
                shipment,
                shipment.getStatus(),
                TRACKING_POD_RECORDED,
                location
        );

        log.info("Proof of delivery ID: {} recorded successfully for delivery ID: {}", savedPod.getId(), deliveryId);
        return ProofOfDeliveryResponse.fromEntity(savedPod);
    }

    @Transactional(readOnly = true)
    public ProofOfDeliveryResponse getProofOfDelivery(Long deliveryId) {
        log.info("Fetching proof of delivery for delivery ID: {}", deliveryId);

        if (!deliveryRepository.existsById(deliveryId)) {
            throw new ResourceNotFoundException("Delivery not found with ID: " + deliveryId);
        }

        ProofOfDelivery pod = proofOfDeliveryRepository.findByDeliveryId(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Proof of delivery not found for delivery ID: " + deliveryId));

        return ProofOfDeliveryResponse.fromEntity(pod);
    }

    @Transactional(readOnly = true)
    public ProofOfDeliveryResponse getProofOfDeliveryByShipment(Long shipmentId) {
        log.info("Fetching proof of delivery for shipment ID: {}", shipmentId);

        if (!shipmentRepository.existsById(shipmentId)) {
            throw new ResourceNotFoundException("Shipment not found with ID: " + shipmentId);
        }

        ProofOfDelivery pod = proofOfDeliveryRepository.findByShipmentId(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Proof of delivery not found for shipment ID: " + shipmentId));

        return ProofOfDeliveryResponse.fromEntity(pod);
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
