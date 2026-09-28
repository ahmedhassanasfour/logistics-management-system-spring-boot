package com.ahmed.logistics.payment.cod.dto;

import com.ahmed.logistics.payment.cod.entity.CashOnDelivery;
import com.ahmed.logistics.payment.cod.entity.CodStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CodResponse(
        Long id,
        Long paymentId,
        Long shipmentId,
        String trackingNumber,
        Long deliveryId,
        Long driverId,
        BigDecimal amountToCollect,
        BigDecimal collectedAmount,
        CodStatus status,
        LocalDateTime collectedAt,
        Long collectedByDriverId,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CodResponse fromEntity(CashOnDelivery cod) {
        if (cod == null) {
            return null;
        }

        Long paymentId = null;
        Long shipmentId = null;
        String trackingNumber = null;
        Long deliveryId = null;
        Long driverId = null;
        Long collectedByDriverId = null;

        if (cod.getPayment() != null) {
            paymentId = cod.getPayment().getId();
            if (cod.getPayment().getShipment() != null) {
                shipmentId = cod.getPayment().getShipment().getId();
                trackingNumber = cod.getPayment().getShipment().getTrackingNumber();
            }
        }

        if (cod.getDelivery() != null) {
            deliveryId = cod.getDelivery().getId();
            if (cod.getDelivery().getDriver() != null) {
                driverId = cod.getDelivery().getDriver().getId();
            }
            if (shipmentId == null && cod.getDelivery().getShipment() != null) {
                shipmentId = cod.getDelivery().getShipment().getId();
                trackingNumber = cod.getDelivery().getShipment().getTrackingNumber();
            }
        }

        if (cod.getCollectedByDriver() != null) {
            collectedByDriverId = cod.getCollectedByDriver().getId();
        }

        return new CodResponse(
                cod.getId(),
                paymentId,
                shipmentId,
                trackingNumber,
                deliveryId,
                driverId,
                cod.getAmountToCollect(),
                cod.getCollectedAmount(),
                cod.getStatus(),
                cod.getCollectedAt(),
                collectedByDriverId,
                cod.getNotes(),
                cod.getCreatedAt(),
                cod.getUpdatedAt()
        );
    }
}
