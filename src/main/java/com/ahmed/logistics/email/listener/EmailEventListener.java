package com.ahmed.logistics.email.listener;

import com.ahmed.logistics.email.service.EmailRecipientResolver;
import com.ahmed.logistics.email.service.EmailService;
import com.ahmed.logistics.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailEventListener {

    private final EmailService emailService;
    private final EmailRecipientResolver recipientResolver;

    @EventListener
    public void handleShipmentDelivered(ShipmentDeliveredEvent event) {
        log.info("Processing email notification for ShipmentDeliveredEvent (shipment ID: {})", event.shipmentId());

        try {
            Optional<String> recipientOpt = recipientResolver.resolveCustomerEmail(event.customerId());
            if (recipientOpt.isEmpty()) {
                log.warn("Cannot send email for ShipmentDeliveredEvent: recipient email not found for customer ID: {}", event.customerId());
                return;
            }

            String recipient = recipientOpt.get();
            String subject = String.format("Shipment Delivered - %s", event.trackingNumber());
            String body = String.format(
                    """
                    Dear Customer,

                    Your shipment has been successfully delivered!

                    Details:
                    - Shipment ID: %d
                    - Tracking Number: %s

                    Thank you for choosing our logistics service.

                    Best regards,
                    Logistics Team
                    """,
                    event.shipmentId(),
                    event.trackingNumber()
            );

            emailService.sendEmail(recipient, subject, body);
        } catch (Exception ex) {
            log.error("Unexpected error in EmailEventListener while handling ShipmentDeliveredEvent for shipment ID: {}. Error: {}",
                    event.shipmentId(), ex.getMessage(), ex);
        }
    }

    @EventListener
    public void handlePaymentPaid(PaymentPaidEvent event) {
        log.info("Processing email notification for PaymentPaidEvent (payment ID: {})", event.paymentId());

        try {
            Optional<String> recipientOpt = recipientResolver.resolveCustomerEmail(event.customerId());
            if (recipientOpt.isEmpty()) {
                log.warn("Cannot send email for PaymentPaidEvent: recipient email not found for customer ID: {}", event.customerId());
                return;
            }

            String recipient = recipientOpt.get();
            String subject = String.format("Payment Confirmed - Shipment #%d", event.shipmentId());
            String amountStr = event.amount() != null ? event.amount().toPlainString() : "0.00";
            String body = String.format(
                    """
                    Dear Customer,

                    We are pleased to inform you that your payment was successfully processed!

                    Payment Details:
                    - Payment ID: %d
                    - Shipment ID: %d
                    - Amount Paid: $%s
                    - Status: PAID

                    Thank you for your payment.

                    Best regards,
                    Logistics Team
                    """,
                    event.paymentId(),
                    event.shipmentId(),
                    amountStr
            );

            emailService.sendEmail(recipient, subject, body);
        } catch (Exception ex) {
            log.error("Unexpected error in EmailEventListener while handling PaymentPaidEvent for payment ID: {}. Error: {}",
                    event.paymentId(), ex.getMessage(), ex);
        }
    }

    @EventListener
    public void handleCodCollected(CodCollectedEvent event) {
        log.info("Processing email notification for CodCollectedEvent (COD ID: {})", event.codId());

        try {
            Optional<String> recipientOpt = recipientResolver.resolveCustomerEmail(event.customerId());
            if (recipientOpt.isEmpty()) {
                log.warn("Cannot send email for CodCollectedEvent: recipient email not found for customer ID: {}", event.customerId());
                return;
            }

            String recipient = recipientOpt.get();
            String subject = String.format("Cash on Delivery Collected - Shipment #%d", event.shipmentId());
            String amountStr = event.amount() != null ? event.amount().toPlainString() : "0.00";
            String body = String.format(
                    """
                    Dear Customer,

                    Your cash on delivery (COD) payment has been collected by our delivery driver.

                    Collection Details:
                    - COD ID: %d
                    - Shipment ID: %d
                    - Amount Collected: $%s
                    - Status: COLLECTED

                    Thank you for your business.

                    Best regards,
                    Logistics Team
                    """,
                    event.codId(),
                    event.shipmentId(),
                    amountStr
            );

            emailService.sendEmail(recipient, subject, body);
        } catch (Exception ex) {
            log.error("Unexpected error in EmailEventListener while handling CodCollectedEvent for COD ID: {}. Error: {}",
                    event.codId(), ex.getMessage(), ex);
        }
    }

    @EventListener
    public void handleDeliveryFailed(DeliveryFailedEvent event) {
        log.info("Processing email notification for DeliveryFailedEvent (delivery ID: {})", event.deliveryId());

        try {
            Optional<String> recipientOpt = recipientResolver.resolveCustomerEmail(event.customerId());
            if (recipientOpt.isEmpty()) {
                log.warn("Cannot send email for DeliveryFailedEvent: recipient email not found for customer ID: {}", event.customerId());
                return;
            }

            String recipient = recipientOpt.get();
            String subject = String.format("Delivery Failed - Shipment #%d", event.shipmentId());
            String reasonStr = event.failureReason() != null ? event.failureReason().name() : "Unspecified";
            String body = String.format(
                    """
                    Dear Customer,

                    We regret to inform you that our delivery attempt for your shipment was unsuccessful.

                    Delivery Details:
                    - Delivery ID: %d
                    - Shipment ID: %d
                    - Failure Reason: %s

                    Our dispatch team will follow up or arrange a reschedule if eligible.

                    Best regards,
                    Logistics Team
                    """,
                    event.deliveryId(),
                    event.shipmentId(),
                    reasonStr
            );

            emailService.sendEmail(recipient, subject, body);
        } catch (Exception ex) {
            log.error("Unexpected error in EmailEventListener while handling DeliveryFailedEvent for delivery ID: {}. Error: {}",
                    event.deliveryId(), ex.getMessage(), ex);
        }
    }

    @EventListener
    public void handleDeliveryRescheduled(DeliveryRescheduledEvent event) {
        log.info("Processing email notification for DeliveryRescheduledEvent (delivery ID: {})", event.deliveryId());

        try {
            Optional<String> recipientOpt = recipientResolver.resolveCustomerEmail(event.customerId());
            if (recipientOpt.isEmpty()) {
                log.warn("Cannot send email for DeliveryRescheduledEvent: recipient email not found for customer ID: {}", event.customerId());
                return;
            }

            String recipient = recipientOpt.get();
            String subject = String.format("Delivery Rescheduled - Shipment #%d", event.shipmentId());
            String scheduledStr = event.newScheduledDateTime() != null ? event.newScheduledDateTime().toString() : "To be determined";
            String reasonStr = event.reason() != null && !event.reason().isBlank() ? event.reason() : "Customer request / Operational adjustment";
            String body = String.format(
                    """
                    Dear Customer,

                    The delivery for your shipment has been rescheduled.

                    Reschedule Details:
                    - Delivery ID: %d
                    - Shipment ID: %d
                    - New Scheduled Date/Time: %s
                    - Reason: %s

                    Thank you for your patience and understanding.

                    Best regards,
                    Logistics Team
                    """,
                    event.deliveryId(),
                    event.shipmentId(),
                    scheduledStr,
                    reasonStr
            );

            emailService.sendEmail(recipient, subject, body);
        } catch (Exception ex) {
            log.error("Unexpected error in EmailEventListener while handling DeliveryRescheduledEvent for delivery ID: {}. Error: {}",
                    event.deliveryId(), ex.getMessage(), ex);
        }
    }
}
