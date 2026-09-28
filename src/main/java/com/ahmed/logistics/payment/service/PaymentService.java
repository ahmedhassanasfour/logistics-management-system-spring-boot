package com.ahmed.logistics.payment.service;

import com.ahmed.logistics.exception.BadRequestException;
import com.ahmed.logistics.exception.ResourceNotFoundException;
import com.ahmed.logistics.payment.dto.CreatePaymentRequest;
import com.ahmed.logistics.payment.dto.PaymentResponse;
import com.ahmed.logistics.payment.entity.Payment;
import com.ahmed.logistics.payment.entity.PaymentStatus;
import com.ahmed.logistics.payment.repository.PaymentRepository;
import com.ahmed.logistics.shipment.entity.Shipment;
import com.ahmed.logistics.shipment.entity.ShipmentStatus;
import com.ahmed.logistics.shipment.repository.ShipmentRepository;
import com.ahmed.logistics.shipment.tracking.service.ShipmentTrackingService;
import com.ahmed.logistics.event.PaymentPaidEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentTrackingService shipmentTrackingService;
    private final ApplicationEventPublisher eventPublisher;

    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED_TRANSITIONS = Map.of(
            PaymentStatus.PENDING, Set.of(PaymentStatus.PAID, PaymentStatus.FAILED, PaymentStatus.CANCELLED),
            PaymentStatus.PAID, Set.of(PaymentStatus.REFUNDED),
            PaymentStatus.FAILED, Set.of(PaymentStatus.PENDING)
    );

    private static final String TRACKING_PAYMENT_CREATED = "Payment created";
    private static final String TRACKING_PAYMENT_COMPLETED = "Payment completed";
    private static final String TRACKING_PAYMENT_FAILED = "Payment failed";
    private static final String TRACKING_PAYMENT_REFUNDED = "Payment refunded";
    private static final String TRACKING_PAYMENT_CANCELLED = "Payment cancelled";

    @Transactional
    public PaymentResponse createPayment(Long shipmentId, CreatePaymentRequest request) {
        log.info("Attempting to create payment for shipment ID: {}", shipmentId);

        if (shipmentId == null) {
            throw new BadRequestException("Shipment ID must not be null");
        }
        if (request == null || request.method() == null) {
            throw new BadRequestException("Payment method is required");
        }

        // 1 & 2. Lock and verify Shipment
        Shipment shipment = shipmentRepository.findByIdForUpdate(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + shipmentId));

        // 3. Verify Shipment eligibility
        if (shipment.getStatus() == ShipmentStatus.CANCELLED || shipment.getStatus() == ShipmentStatus.RETURNED) {
            log.warn("Cannot create payment: Shipment ID {} is in ineligible status {}", shipmentId, shipment.getStatus());
            throw new BadRequestException(
                    String.format("Shipment cannot be paid in status: %s", shipment.getStatus())
            );
        }

        // 4. Verify no existing Payment for the Shipment
        if (paymentRepository.existsByShipmentId(shipmentId)) {
            log.warn("Payment already exists for shipment ID: {}", shipmentId);
            throw new BadRequestException("Payment already exists for shipment ID: " + shipmentId);
        }

        // 5, 6 & 7. Get amount only from shipment.getTotalPrice() and validate > 0
        BigDecimal amount = shipment.getTotalPrice();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Cannot create payment: Shipment ID {} total price is invalid ({})", shipmentId, amount);
            throw new BadRequestException("Shipment total price must be greater than zero to create a payment");
        }

        // 8 & 9. Create Payment with PENDING status and unique transaction reference
        String transactionReference = generateUniqueTransactionReference();
        Payment payment = Payment.builder()
                .shipment(shipment)
                .amount(amount)
                .method(request.method())
                .status(PaymentStatus.PENDING)
                .transactionReference(transactionReference)
                .build();

        // 10. Persist Payment
        Payment savedPayment = paymentRepository.save(payment);

        // 11. Record shipment tracking event in same transaction
        String location = buildDeliveryLocation(shipment);
        shipmentTrackingService.recordStatusChange(
                shipment,
                shipment.getStatus(),
                TRACKING_PAYMENT_CREATED,
                location
        );

        log.info("Payment created successfully with ID: {} and reference: {} for shipment ID: {}",
                savedPayment.getId(), savedPayment.getTransactionReference(), shipmentId);
        return PaymentResponse.fromEntity(savedPayment);
    }

    @Transactional
    public PaymentResponse markAsPaid(Long paymentId) {
        log.info("Marking payment ID: {} as PAID", paymentId);

        if (paymentId == null) {
            throw new BadRequestException("Payment ID must not be null");
        }

        // 1. Lock Payment using pessimistic locking
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentId));

        // 2. Validate current status allows transition to PAID
        validateTransition(payment.getStatus(), PaymentStatus.PAID);

        // 3 & 4. Update status and paidAt while preserving amount and method
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        Payment savedPayment = paymentRepository.save(payment);

        // 5. Record shipment tracking event
        Shipment shipment = payment.getShipment();
        if (shipment != null) {
            String location = buildDeliveryLocation(shipment);
            shipmentTrackingService.recordStatusChange(
                    shipment,
                    shipment.getStatus(),
                    TRACKING_PAYMENT_COMPLETED,
                    location
            );
        }

        log.info("Payment ID: {} marked as PAID successfully", paymentId);

        Long shipmentId = shipment != null ? shipment.getId() : null;
        Long customerId = (shipment != null && shipment.getCustomer() != null) ? shipment.getCustomer().getId() : null;
        eventPublisher.publishEvent(new PaymentPaidEvent(savedPayment.getId(), shipmentId, customerId, savedPayment.getAmount()));

        return PaymentResponse.fromEntity(savedPayment);
    }

    @Transactional
    public PaymentResponse markAsFailed(Long paymentId) {
        log.info("Marking payment ID: {} as FAILED", paymentId);

        if (paymentId == null) {
            throw new BadRequestException("Payment ID must not be null");
        }

        // 1. Lock Payment
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentId));

        // 2. Validate transition
        validateTransition(payment.getStatus(), PaymentStatus.FAILED);

        // 3. Update status to FAILED
        payment.setStatus(PaymentStatus.FAILED);
        Payment savedPayment = paymentRepository.save(payment);

        // 4. Record tracking event
        Shipment shipment = payment.getShipment();
        if (shipment != null) {
            String location = buildDeliveryLocation(shipment);
            shipmentTrackingService.recordStatusChange(
                    shipment,
                    shipment.getStatus(),
                    TRACKING_PAYMENT_FAILED,
                    location
            );
        }

        log.info("Payment ID: {} marked as FAILED successfully", paymentId);
        return PaymentResponse.fromEntity(savedPayment);
    }

    @Transactional
    public PaymentResponse refundPayment(Long paymentId) {
        log.info("Refunding payment ID: {}", paymentId);

        if (paymentId == null) {
            throw new BadRequestException("Payment ID must not be null");
        }

        // 1. Lock Payment
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentId));

        // 2. Validate transition (only PAID -> REFUNDED)
        validateTransition(payment.getStatus(), PaymentStatus.REFUNDED);

        // 3. Update status to REFUNDED and refundedAt
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(LocalDateTime.now());
        Payment savedPayment = paymentRepository.save(payment);

        // 4. Record tracking event
        Shipment shipment = payment.getShipment();
        if (shipment != null) {
            String location = buildDeliveryLocation(shipment);
            shipmentTrackingService.recordStatusChange(
                    shipment,
                    shipment.getStatus(),
                    TRACKING_PAYMENT_REFUNDED,
                    location
            );
        }

        log.info("Payment ID: {} refunded successfully", paymentId);
        return PaymentResponse.fromEntity(savedPayment);
    }

    @Transactional
    public PaymentResponse cancelPayment(Long paymentId) {
        log.info("Cancelling payment ID: {}", paymentId);

        if (paymentId == null) {
            throw new BadRequestException("Payment ID must not be null");
        }

        // 1. Lock Payment
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentId));

        // 2. Validate transition (only PENDING -> CANCELLED)
        validateTransition(payment.getStatus(), PaymentStatus.CANCELLED);

        // 3. Update status to CANCELLED
        payment.setStatus(PaymentStatus.CANCELLED);
        Payment savedPayment = paymentRepository.save(payment);

        // 4. Record tracking event
        Shipment shipment = payment.getShipment();
        if (shipment != null) {
            String location = buildDeliveryLocation(shipment);
            shipmentTrackingService.recordStatusChange(
                    shipment,
                    shipment.getStatus(),
                    TRACKING_PAYMENT_CANCELLED,
                    location
            );
        }

        log.info("Payment ID: {} cancelled successfully", paymentId);
        return PaymentResponse.fromEntity(savedPayment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId) {
        log.info("Fetching payment with ID: {}", paymentId);

        if (paymentId == null) {
            throw new BadRequestException("Payment ID must not be null");
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentId));

        return PaymentResponse.fromEntity(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByShipmentId(Long shipmentId) {
        log.info("Fetching payment for shipment ID: {}", shipmentId);

        if (shipmentId == null) {
            throw new BadRequestException("Shipment ID must not be null");
        }

        if (!shipmentRepository.existsById(shipmentId)) {
            throw new ResourceNotFoundException("Shipment not found with ID: " + shipmentId);
        }

        Payment payment = paymentRepository.findByShipmentId(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for shipment ID: " + shipmentId));

        return PaymentResponse.fromEntity(payment);
    }

    private void validateTransition(PaymentStatus currentStatus, PaymentStatus targetStatus) {
        Set<PaymentStatus> allowedTargets = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Collections.emptySet());
        if (!allowedTargets.contains(targetStatus)) {
            log.warn("Invalid payment transition requested: {} -> {}", currentStatus, targetStatus);
            throw new BadRequestException(
                    String.format("Cannot transition payment from %s to %s", currentStatus, targetStatus)
            );
        }
    }

    private String generateUniqueTransactionReference() {
        String reference;
        do {
            reference = "PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        } while (paymentRepository.existsByTransactionReference(reference));
        return reference;
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
