package com.ahmed.logistics.payment.dto;

import com.ahmed.logistics.payment.entity.Payment;
import com.ahmed.logistics.payment.entity.PaymentMethod;
import com.ahmed.logistics.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long shipmentId,
        String trackingNumber,
        BigDecimal amount,
        PaymentMethod method,
        PaymentStatus status,
        String transactionReference,
        LocalDateTime paidAt,
        LocalDateTime refundedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PaymentResponse fromEntity(Payment payment) {
        if (payment == null) {
            return null;
        }

        Long shipmentId = null;
        String trackingNumber = null;

        if (payment.getShipment() != null) {
            shipmentId = payment.getShipment().getId();
            trackingNumber = payment.getShipment().getTrackingNumber();
        }

        return new PaymentResponse(
                payment.getId(),
                shipmentId,
                trackingNumber,
                payment.getAmount(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getTransactionReference(),
                payment.getPaidAt(),
                payment.getRefundedAt(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
