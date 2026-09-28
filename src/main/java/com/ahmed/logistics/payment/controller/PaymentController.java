package com.ahmed.logistics.payment.controller;

import com.ahmed.logistics.payment.dto.CreatePaymentRequest;
import com.ahmed.logistics.payment.dto.PaymentResponse;
import com.ahmed.logistics.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PreAuthorize("@paymentSecurity.canCreateForShipment(#shipmentId, authentication)")
    @PostMapping("/api/shipments/{shipmentId}/payment")
    public ResponseEntity<PaymentResponse> createPayment(
            @PathVariable Long shipmentId,
            @Valid @RequestBody CreatePaymentRequest request,
            Authentication authentication
    ) {
        PaymentResponse response = paymentService.createPayment(shipmentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("@paymentSecurity.canReadPayment(#paymentId, authentication)")
    @GetMapping("/api/payments/{paymentId}")
    public ResponseEntity<PaymentResponse> getPaymentById(
            @PathVariable Long paymentId,
            Authentication authentication
    ) {
        PaymentResponse response = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@paymentSecurity.canReadShipmentPayment(#shipmentId, authentication)")
    @GetMapping("/api/shipments/{shipmentId}/payment")
    public ResponseEntity<PaymentResponse> getPaymentByShipmentId(
            @PathVariable Long shipmentId,
            Authentication authentication
    ) {
        PaymentResponse response = paymentService.getPaymentByShipmentId(shipmentId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@paymentSecurity.canManagePayment(authentication)")
    @PatchMapping("/api/payments/{paymentId}/pay")
    public ResponseEntity<PaymentResponse> markAsPaid(
            @PathVariable Long paymentId
    ) {
        PaymentResponse response = paymentService.markAsPaid(paymentId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@paymentSecurity.canManagePayment(authentication)")
    @PatchMapping("/api/payments/{paymentId}/fail")
    public ResponseEntity<PaymentResponse> markAsFailed(
            @PathVariable Long paymentId
    ) {
        PaymentResponse response = paymentService.markAsFailed(paymentId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@paymentSecurity.canManagePayment(authentication)")
    @PatchMapping("/api/payments/{paymentId}/refund")
    public ResponseEntity<PaymentResponse> refundPayment(
            @PathVariable Long paymentId
    ) {
        PaymentResponse response = paymentService.refundPayment(paymentId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@paymentSecurity.canManagePayment(authentication)")
    @PatchMapping("/api/payments/{paymentId}/cancel")
    public ResponseEntity<PaymentResponse> cancelPayment(
            @PathVariable Long paymentId
    ) {
        PaymentResponse response = paymentService.cancelPayment(paymentId);
        return ResponseEntity.ok(response);
    }
}
