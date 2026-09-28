package com.ahmed.logistics.payment.controller;

import com.ahmed.logistics.common.idempotency.service.IdempotencyHashService;
import com.ahmed.logistics.common.idempotency.service.IdempotencyService;
import com.ahmed.logistics.payment.dto.CreatePaymentRequest;
import com.ahmed.logistics.payment.dto.PaymentResponse;
import com.ahmed.logistics.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Payments", description = "Electronic payments, refund management, and idempotency protection")
@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final IdempotencyService idempotencyService;
    private final IdempotencyHashService idempotencyHashService;

    @Operation(summary = "Create shipment payment", description = "Initiates an electronic payment for a shipment. Supports idempotent requests via the Idempotency-Key header.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payment created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid payment payload or shipment already paid"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - customer ID mismatch"),
            @ApiResponse(responseCode = "404", description = "Shipment not found"),
            @ApiResponse(responseCode = "409", description = "Conflict - concurrent idempotent request in progress")
    })
    @PreAuthorize("@paymentSecurity.canCreateForShipment(#shipmentId, authentication)")
    @PostMapping("/api/shipments/{shipmentId}/payment")
    public ResponseEntity<PaymentResponse> createPayment(
            @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, description = "Unique idempotency key (UUID) to prevent duplicate charges", required = false)
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Parameter(description = "Shipment ID", required = true)
            @PathVariable Long shipmentId,
            @Valid @RequestBody CreatePaymentRequest request,
            Authentication authentication
    ) {
        String requestHash = idempotencyHashService.hashPaymentCreation(shipmentId, request);
        PaymentResponse response = idempotencyService.execute(
                idempotencyKey,
                "CREATE_PAYMENT",
                requestHash,
                authentication != null ? authentication.getName() : null,
                PaymentResponse.class,
                HttpStatus.CREATED.value(),
                () -> paymentService.createPayment(shipmentId, request)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get payment by ID", description = "Retrieves payment details by payment ID.")
    @PreAuthorize("@paymentSecurity.canReadPayment(#paymentId, authentication)")
    @GetMapping("/api/payments/{paymentId}")
    public ResponseEntity<PaymentResponse> getPaymentById(
            @Parameter(description = "Payment ID", required = true)
            @PathVariable Long paymentId,
            Authentication authentication
    ) {
        PaymentResponse response = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get payment by shipment ID", description = "Retrieves payment details for a specific shipment.")
    @PreAuthorize("@paymentSecurity.canReadShipmentPayment(#shipmentId, authentication)")
    @GetMapping("/api/shipments/{shipmentId}/payment")
    public ResponseEntity<PaymentResponse> getPaymentByShipmentId(
            @Parameter(description = "Shipment ID", required = true)
            @PathVariable Long shipmentId,
            Authentication authentication
    ) {
        PaymentResponse response = paymentService.getPaymentByShipmentId(shipmentId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Mark payment as paid", description = "Transitions payment status to PAID and triggers payment domain events. Requires ADMIN or DISPATCHER role.")
    @PreAuthorize("@paymentSecurity.canManagePayment(authentication)")
    @PatchMapping("/api/payments/{paymentId}/pay")
    public ResponseEntity<PaymentResponse> markAsPaid(
            @Parameter(description = "Payment ID", required = true)
            @PathVariable Long paymentId
    ) {
        PaymentResponse response = paymentService.markAsPaid(paymentId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Mark payment as failed", description = "Records a payment failure. Requires ADMIN or DISPATCHER role.")
    @PreAuthorize("@paymentSecurity.canManagePayment(authentication)")
    @PatchMapping("/api/payments/{paymentId}/fail")
    public ResponseEntity<PaymentResponse> markAsFailed(
            @Parameter(description = "Payment ID", required = true)
            @PathVariable Long paymentId
    ) {
        PaymentResponse response = paymentService.markAsFailed(paymentId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Refund payment", description = "Issues a refund for a previously paid payment. Requires ADMIN or DISPATCHER role.")
    @PreAuthorize("@paymentSecurity.canManagePayment(authentication)")
    @PatchMapping("/api/payments/{paymentId}/refund")
    public ResponseEntity<PaymentResponse> refundPayment(
            @Parameter(description = "Payment ID", required = true)
            @PathVariable Long paymentId
    ) {
        PaymentResponse response = paymentService.refundPayment(paymentId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cancel payment", description = "Cancels a pending payment. Requires ADMIN or DISPATCHER role.")
    @PreAuthorize("@paymentSecurity.canManagePayment(authentication)")
    @PatchMapping("/api/payments/{paymentId}/cancel")
    public ResponseEntity<PaymentResponse> cancelPayment(
            @Parameter(description = "Payment ID", required = true)
            @PathVariable Long paymentId
    ) {
        PaymentResponse response = paymentService.cancelPayment(paymentId);
        return ResponseEntity.ok(response);
    }
}
