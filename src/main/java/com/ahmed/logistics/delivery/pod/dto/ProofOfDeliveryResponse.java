package com.ahmed.logistics.delivery.pod.dto;

import com.ahmed.logistics.delivery.pod.entity.ProofOfDelivery;

import java.time.LocalDateTime;

public record ProofOfDeliveryResponse(
        Long id,
        Long deliveryId,
        Long shipmentId,
        String trackingNumber,
        String recipientName,
        String recipientPhone,
        String recipientId,
        String notes,
        LocalDateTime confirmedAt,
        LocalDateTime createdAt
) {
    public static ProofOfDeliveryResponse fromEntity(ProofOfDelivery pod) {
        if (pod == null) {
            return null;
        }

        Long deliveryId = null;
        Long shipmentId = null;
        String trackingNumber = null;

        if (pod.getDelivery() != null) {
            deliveryId = pod.getDelivery().getId();
            if (pod.getDelivery().getShipment() != null) {
                shipmentId = pod.getDelivery().getShipment().getId();
                trackingNumber = pod.getDelivery().getShipment().getTrackingNumber();
            }
        }

        return new ProofOfDeliveryResponse(
                pod.getId(),
                deliveryId,
                shipmentId,
                trackingNumber,
                pod.getRecipientName(),
                pod.getRecipientPhone(),
                pod.getRecipientId(),
                pod.getNotes(),
                pod.getConfirmedAt(),
                pod.getCreatedAt()
        );
    }
}
