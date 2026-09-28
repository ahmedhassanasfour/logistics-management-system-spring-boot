package com.ahmed.logistics.notification.listener;

import com.ahmed.logistics.event.*;
import com.ahmed.logistics.notification.entity.NotificationType;
import com.ahmed.logistics.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleShipmentDelivered(ShipmentDeliveredEvent event) {
        log.info("Received ShipmentDeliveredEvent asynchronously for shipment ID: {}", event.shipmentId());

        if (event.customerId() == null) {
            log.warn("Cannot create notification for ShipmentDeliveredEvent: customerId is null (shipment ID: {})", event.shipmentId());
            return;
        }

        String title = "Shipment Delivered";
        String message = String.format("Your shipment with tracking number %s has been successfully delivered.", event.trackingNumber());

        try {
            notificationService.createNotification(event.customerId(), NotificationType.SHIPMENT_DELIVERED, title, message);
        } catch (Exception ex) {
            log.error("Failed to create notification for ShipmentDeliveredEvent (shipment ID: {}): {}",
                    event.shipmentId(), ex.getMessage(), ex);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentPaid(PaymentPaidEvent event) {
        log.info("Received PaymentPaidEvent asynchronously for payment ID: {}", event.paymentId());

        if (event.customerId() == null) {
            log.warn("Cannot create notification for PaymentPaidEvent: customerId is null (payment ID: {})", event.paymentId());
            return;
        }

        String title = "Payment Confirmed";
        String amountStr = event.amount() != null ? event.amount().toPlainString() : "0.00";
        String message = String.format("Payment #%d of $%s for shipment #%d has been successfully confirmed.",
                event.paymentId(), amountStr, event.shipmentId());

        try {
            notificationService.createNotification(event.customerId(), NotificationType.PAYMENT_PAID, title, message);
        } catch (Exception ex) {
            log.error("Failed to create notification for PaymentPaidEvent (payment ID: {}): {}",
                    event.paymentId(), ex.getMessage(), ex);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCodCollected(CodCollectedEvent event) {
        log.info("Received CodCollectedEvent asynchronously for COD ID: {}", event.codId());

        if (event.customerId() == null) {
            log.warn("Cannot create notification for CodCollectedEvent: customerId is null (COD ID: {})", event.codId());
            return;
        }

        String title = "Cash on Delivery Collected";
        String amountStr = event.amount() != null ? event.amount().toPlainString() : "0.00";
        String message = String.format("Cash on delivery payment of $%s for shipment #%d has been successfully collected.",
                amountStr, event.shipmentId());

        try {
            notificationService.createNotification(event.customerId(), NotificationType.COD_COLLECTED, title, message);
        } catch (Exception ex) {
            log.error("Failed to create notification for CodCollectedEvent (COD ID: {}): {}",
                    event.codId(), ex.getMessage(), ex);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDeliveryFailed(DeliveryFailedEvent event) {
        log.info("Received DeliveryFailedEvent asynchronously for delivery ID: {}", event.deliveryId());

        if (event.customerId() == null) {
            log.warn("Cannot create notification for DeliveryFailedEvent: customerId is null (delivery ID: {})", event.deliveryId());
            return;
        }

        String title = "Delivery Failed";
        String reasonStr = event.failureReason() != null ? event.failureReason().name() : "Unspecified";
        String message = String.format("Delivery attempt for shipment #%d failed. Reason: %s.",
                event.shipmentId(), reasonStr);

        try {
            notificationService.createNotification(event.customerId(), NotificationType.DELIVERY_FAILED, title, message);
        } catch (Exception ex) {
            log.error("Failed to create notification for DeliveryFailedEvent (delivery ID: {}): {}",
                    event.deliveryId(), ex.getMessage(), ex);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDeliveryRescheduled(DeliveryRescheduledEvent event) {
        log.info("Received DeliveryRescheduledEvent asynchronously for delivery ID: {}", event.deliveryId());

        if (event.customerId() == null) {
            log.warn("Cannot create notification for DeliveryRescheduledEvent: customerId is null (delivery ID: {})", event.deliveryId());
            return;
        }

        String title = "Delivery Rescheduled";
        String scheduledStr = event.newScheduledDateTime() != null ? event.newScheduledDateTime().toString() : "a future date";
        String message = String.format("Delivery for shipment #%d has been rescheduled to %s.",
                event.shipmentId(), scheduledStr);

        try {
            notificationService.createNotification(event.customerId(), NotificationType.DELIVERY_RESCHEDULED, title, message);
        } catch (Exception ex) {
            log.error("Failed to create notification for DeliveryRescheduledEvent (delivery ID: {}): {}",
                    event.deliveryId(), ex.getMessage(), ex);
        }
    }
}
