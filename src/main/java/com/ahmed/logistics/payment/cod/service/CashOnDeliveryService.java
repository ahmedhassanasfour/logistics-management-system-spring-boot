package com.ahmed.logistics.payment.cod.service;

import com.ahmed.logistics.delivery.entity.Delivery;
import com.ahmed.logistics.delivery.entity.DeliveryStatus;
import com.ahmed.logistics.delivery.repository.DeliveryRepository;
import com.ahmed.logistics.driver.entity.Driver;
import com.ahmed.logistics.driver.repository.DriverRepository;
import com.ahmed.logistics.exception.BadRequestException;
import com.ahmed.logistics.exception.ResourceNotFoundException;
import com.ahmed.logistics.payment.cod.dto.CreateCodRequest;
import com.ahmed.logistics.payment.cod.dto.CodResponse;
import com.ahmed.logistics.payment.cod.entity.CashOnDelivery;
import com.ahmed.logistics.payment.cod.entity.CodStatus;
import com.ahmed.logistics.payment.cod.repository.CashOnDeliveryRepository;
import com.ahmed.logistics.payment.entity.Payment;
import com.ahmed.logistics.payment.entity.PaymentMethod;
import com.ahmed.logistics.payment.entity.PaymentStatus;
import com.ahmed.logistics.payment.repository.PaymentRepository;
import com.ahmed.logistics.shipment.entity.Shipment;
import com.ahmed.logistics.shipment.repository.ShipmentRepository;
import com.ahmed.logistics.shipment.tracking.service.ShipmentTrackingService;
import com.ahmed.logistics.event.CodCollectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashOnDeliveryService {

    private final CashOnDeliveryRepository cashOnDeliveryRepository;
    private final PaymentRepository paymentRepository;
    private final DeliveryRepository deliveryRepository;
    private final ShipmentRepository shipmentRepository;
    private final DriverRepository driverRepository;
    private final ShipmentTrackingService shipmentTrackingService;
    private final ApplicationEventPublisher eventPublisher;

    private static final String TRACKING_COD_CREATED = "COD payment created";
    private static final String TRACKING_COD_COLLECTED = "COD payment collected";
    private static final String TRACKING_COD_FAILED = "COD payment failed";
    private static final String TRACKING_COD_CANCELLED = "COD payment cancelled";

    @Transactional
    public CodResponse createCod(Long deliveryId, CreateCodRequest request) {
        log.info("Creating COD for delivery ID: {}", deliveryId);

        if (deliveryId == null) {
            throw new BadRequestException("Delivery ID must not be null");
        }

        // 1 & 2. Lock and verify Delivery
        Delivery delivery = deliveryRepository.findByIdForUpdate(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found with ID: " + deliveryId));

        // 3. Verify its Shipment
        Shipment shipment = delivery.getShipment();
        if (shipment == null) {
            throw new BadRequestException("Delivery is not associated with a valid shipment");
        }

        Shipment lockedShipment = shipmentRepository.findByIdForUpdate(shipment.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + shipment.getId()));

        // 4 & 5. Find Payment for the Shipment
        Payment payment = paymentRepository.findByShipmentIdForUpdate(lockedShipment.getId())
                .orElseThrow(() -> new BadRequestException("Shipment does not have an associated payment"));

        // 6. Payment method MUST be CASH
        if (payment.getMethod() != PaymentMethod.CASH) {
            log.warn("Cannot create COD: Payment ID {} method is {}, expected CASH", payment.getId(), payment.getMethod());
            throw new BadRequestException("COD is only available for CASH payments");
        }

        // 7. Payment must be PENDING
        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.warn("Cannot create COD: Payment ID {} status is {}, expected PENDING", payment.getId(), payment.getStatus());
            throw new BadRequestException(
                    String.format("Payment must be in PENDING status to create COD. Current status: %s", payment.getStatus())
            );
        }

        // 8. Delivery must be a valid delivery attempt for this Shipment
        if (!delivery.getShipment().getId().equals(lockedShipment.getId())) {
            throw new BadRequestException("Delivery attempt does not belong to the shipment");
        }

        // 9. Do not allow duplicate COD for the same Payment
        if (cashOnDeliveryRepository.existsByPaymentId(payment.getId())) {
            log.warn("COD already exists for payment ID: {}", payment.getId());
            throw new BadRequestException("COD record already exists for this payment");
        }

        // 10. Build COD entity
        CashOnDelivery cod = CashOnDelivery.builder()
                .payment(payment)
                .delivery(delivery)
                .amountToCollect(payment.getAmount())
                .status(CodStatus.PENDING)
                .collectedAmount(null)
                .notes(request != null ? request.notes() : null)
                .build();

        // 11. Persist COD
        CashOnDelivery savedCod = cashOnDeliveryRepository.save(cod);

        // 12. Record shipment tracking event in same transaction
        String location = buildDeliveryLocation(lockedShipment);
        shipmentTrackingService.recordStatusChange(
                lockedShipment,
                lockedShipment.getStatus(),
                TRACKING_COD_CREATED,
                location
        );

        log.info("COD record created successfully with ID: {} for delivery ID: {}", savedCod.getId(), deliveryId);
        return CodResponse.fromEntity(savedCod);
    }

    @Transactional
    public CodResponse collectCod(Long codId, BigDecimal collectedAmount, String notes, Authentication authentication) {
        log.info("Attempting to collect COD ID: {}", codId);

        if (codId == null) {
            throw new BadRequestException("COD ID must not be null");
        }
        if (collectedAmount == null || collectedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Collected amount must be greater than zero");
        }

        // 1. Lock COD
        CashOnDelivery cod = cashOnDeliveryRepository.findByIdForUpdate(codId)
                .orElseThrow(() -> new ResourceNotFoundException("COD record not found with ID: " + codId));

        // 2. Validate COD status
        if (cod.getStatus() != CodStatus.PENDING) {
            log.warn("Cannot collect COD: COD ID {} is in status {}, expected PENDING", codId, cod.getStatus());
            throw new BadRequestException(
                    String.format("COD must be in PENDING status to be collected. Current status: %s", cod.getStatus())
            );
        }

        // 3. Lock Payment
        Payment payment = paymentRepository.findByIdForUpdate(cod.getPayment().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + cod.getPayment().getId()));

        // 4. Validate Payment status
        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.warn("Cannot collect COD: Payment ID {} is in status {}, expected PENDING", payment.getId(), payment.getStatus());
            throw new BadRequestException(
                    String.format("Payment must be in PENDING status to collect COD. Current status: %s", payment.getStatus())
            );
        }

        // 5. Lock Delivery
        Delivery delivery = deliveryRepository.findByIdForUpdate(cod.getDelivery().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found with ID: " + cod.getDelivery().getId()));

        // 6. Validate Delivery status is IN_PROGRESS
        if (delivery.getStatus() != DeliveryStatus.IN_PROGRESS) {
            log.warn("Cannot collect COD: Delivery ID {} is in status {}, expected IN_PROGRESS", delivery.getId(), delivery.getStatus());
            throw new BadRequestException(
                    String.format("Delivery must be IN_PROGRESS to collect COD. Current status: %s", delivery.getStatus())
            );
        }

        // 7. Lock Shipment
        Shipment shipment = shipmentRepository.findByIdForUpdate(delivery.getShipment().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + delivery.getShipment().getId()));

        // 8. Driver and actor validation
        boolean isStaff = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_DISPATCHER"));

        Driver collectingDriver = delivery.getDriver();
        if (!isStaff) {
            if (collectingDriver == null || collectingDriver.getUser() == null || authentication == null
                    || !collectingDriver.getUser().getEmail().equalsIgnoreCase(authentication.getName())) {
                log.warn("Authenticated user {} is not the assigned driver for delivery ID {}",
                        authentication != null ? authentication.getName() : "null", delivery.getId());
                throw new BadRequestException("Only the assigned driver can collect COD for this delivery");
            }
        }

        if (collectingDriver == null) {
            throw new BadRequestException("Delivery does not have an assigned driver to collect COD");
        }

        Driver lockedDriver = driverRepository.findByIdForUpdate(collectingDriver.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with ID: " + collectingDriver.getId()));

        // 9. Validate collected amount equals amountToCollect
        if (collectedAmount.compareTo(cod.getAmountToCollect()) != 0) {
            log.warn("Collected amount {} does not equal required COD amount {}", collectedAmount, cod.getAmountToCollect());
            throw new BadRequestException(
                    String.format("Collected amount (%s) must equal COD amount (%s)", collectedAmount, cod.getAmountToCollect())
            );
        }

        // 10. Update COD
        cod.setStatus(CodStatus.COLLECTED);
        cod.setCollectedAmount(cod.getAmountToCollect());
        cod.setCollectedAt(LocalDateTime.now());
        cod.setCollectedByDriver(lockedDriver);
        if (notes != null && !notes.isBlank()) {
            cod.setNotes(notes.trim());
        }
        CashOnDelivery savedCod = cashOnDeliveryRepository.save(cod);

        // 11. Update Payment (PENDING -> PAID)
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // 12. Record shipment tracking
        String location = buildDeliveryLocation(shipment);
        shipmentTrackingService.recordStatusChange(
                shipment,
                shipment.getStatus(),
                TRACKING_COD_COLLECTED,
                location
        );

        log.info("COD ID: {} collected successfully by driver ID: {}", savedCod.getId(), lockedDriver.getId());

        Long customerId = shipment.getCustomer() != null ? shipment.getCustomer().getId() : null;
        eventPublisher.publishEvent(new CodCollectedEvent(savedCod.getId(), shipment.getId(), customerId, savedCod.getCollectedAmount()));

        return CodResponse.fromEntity(savedCod);
    }

    @Transactional
    public CodResponse markAsFailed(Long codId, String notes) {
        log.info("Marking COD ID: {} as FAILED", codId);

        if (codId == null) {
            throw new BadRequestException("COD ID must not be null");
        }

        CashOnDelivery cod = cashOnDeliveryRepository.findByIdForUpdate(codId)
                .orElseThrow(() -> new ResourceNotFoundException("COD record not found with ID: " + codId));

        if (cod.getStatus() != CodStatus.PENDING) {
            log.warn("Cannot fail COD: COD ID {} is in status {}, expected PENDING", codId, cod.getStatus());
            throw new BadRequestException(
                    String.format("Cannot fail COD from status: %s", cod.getStatus())
            );
        }

        cod.setStatus(CodStatus.FAILED);
        if (notes != null && !notes.isBlank()) {
            cod.setNotes(notes.trim());
        }
        CashOnDelivery savedCod = cashOnDeliveryRepository.save(cod);

        Shipment shipment = cod.getDelivery() != null ? cod.getDelivery().getShipment() : null;
        if (shipment != null) {
            String location = buildDeliveryLocation(shipment);
            shipmentTrackingService.recordStatusChange(
                    shipment,
                    shipment.getStatus(),
                    TRACKING_COD_FAILED,
                    location
            );
        }

        log.info("COD ID: {} marked as FAILED successfully", codId);
        return CodResponse.fromEntity(savedCod);
    }

    @Transactional
    public CodResponse cancelCod(Long codId) {
        log.info("Cancelling COD ID: {}", codId);

        if (codId == null) {
            throw new BadRequestException("COD ID must not be null");
        }

        CashOnDelivery cod = cashOnDeliveryRepository.findByIdForUpdate(codId)
                .orElseThrow(() -> new ResourceNotFoundException("COD record not found with ID: " + codId));

        if (cod.getStatus() != CodStatus.PENDING) {
            log.warn("Cannot cancel COD: COD ID {} is in status {}, expected PENDING", codId, cod.getStatus());
            throw new BadRequestException(
                    String.format("Cannot cancel COD from status: %s", cod.getStatus())
            );
        }

        cod.setStatus(CodStatus.CANCELLED);
        CashOnDelivery savedCod = cashOnDeliveryRepository.save(cod);

        Shipment shipment = cod.getDelivery() != null ? cod.getDelivery().getShipment() : null;
        if (shipment != null) {
            String location = buildDeliveryLocation(shipment);
            shipmentTrackingService.recordStatusChange(
                    shipment,
                    shipment.getStatus(),
                    TRACKING_COD_CANCELLED,
                    location
            );
        }

        log.info("COD ID: {} cancelled successfully", codId);
        return CodResponse.fromEntity(savedCod);
    }

    @Transactional(readOnly = true)
    public CodResponse getCodById(Long codId) {
        log.info("Fetching COD with ID: {}", codId);

        if (codId == null) {
            throw new BadRequestException("COD ID must not be null");
        }

        CashOnDelivery cod = cashOnDeliveryRepository.findById(codId)
                .orElseThrow(() -> new ResourceNotFoundException("COD record not found with ID: " + codId));

        return CodResponse.fromEntity(cod);
    }

    @Transactional(readOnly = true)
    public CodResponse getCodByDeliveryId(Long deliveryId) {
        log.info("Fetching COD for delivery ID: {}", deliveryId);

        if (deliveryId == null) {
            throw new BadRequestException("Delivery ID must not be null");
        }

        if (!deliveryRepository.existsById(deliveryId)) {
            throw new ResourceNotFoundException("Delivery not found with ID: " + deliveryId);
        }

        CashOnDelivery cod = cashOnDeliveryRepository.findByDeliveryId(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("COD record not found for delivery ID: " + deliveryId));

        return CodResponse.fromEntity(cod);
    }

    @Transactional(readOnly = true)
    public CodResponse getCodByPaymentId(Long paymentId) {
        log.info("Fetching COD for payment ID: {}", paymentId);

        if (paymentId == null) {
            throw new BadRequestException("Payment ID must not be null");
        }

        CashOnDelivery cod = cashOnDeliveryRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("COD record not found for payment ID: " + paymentId));

        return CodResponse.fromEntity(cod);
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
