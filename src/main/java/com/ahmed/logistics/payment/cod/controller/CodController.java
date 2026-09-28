package com.ahmed.logistics.payment.cod.controller;

import com.ahmed.logistics.common.idempotency.service.IdempotencyHashService;
import com.ahmed.logistics.common.idempotency.service.IdempotencyService;
import com.ahmed.logistics.payment.cod.dto.CollectCodRequest;
import com.ahmed.logistics.payment.cod.dto.CreateCodRequest;
import com.ahmed.logistics.payment.cod.dto.FailCodRequest;
import com.ahmed.logistics.payment.cod.dto.CodResponse;
import com.ahmed.logistics.payment.cod.service.CashOnDeliveryService;
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

@Tag(name = "Cash on Delivery", description = "Cash on delivery order creation, driver cash collection, and idempotency protection")
@RestController
@RequiredArgsConstructor
public class CodController {

    private final CashOnDeliveryService cashOnDeliveryService;
    private final IdempotencyService idempotencyService;
    private final IdempotencyHashService idempotencyHashService;

    @Operation(summary = "Create COD order", description = "Creates a cash on delivery record linked to a delivery. Restricted to driver, customer owner, or staff.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "COD record created"),
            @ApiResponse(responseCode = "400", description = "Invalid payload or delivery not eligible for COD"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - access denied"),
            @ApiResponse(responseCode = "404", description = "Delivery not found")
    })
    @PreAuthorize("@codSecurity.canCreate(#deliveryId, authentication)")
    @PostMapping("/api/deliveries/{deliveryId}/cod")
    public ResponseEntity<CodResponse> createCod(
            @Parameter(description = "Delivery ID", required = true)
            @PathVariable Long deliveryId,
            @Valid @RequestBody(required = false) CreateCodRequest request
    ) {
        CodResponse response = cashOnDeliveryService.createCod(deliveryId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get COD by ID", description = "Retrieves COD record details by COD ID.")
    @PreAuthorize("@codSecurity.canRead(#codId, authentication)")
    @GetMapping("/api/cod/{codId}")
    public ResponseEntity<CodResponse> getCodById(
            @Parameter(description = "COD ID", required = true)
            @PathVariable Long codId,
            Authentication authentication
    ) {
        CodResponse response = cashOnDeliveryService.getCodById(codId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get COD by Delivery ID", description = "Retrieves COD record details for a specific delivery.")
    @PreAuthorize("@codSecurity.canReadByDelivery(#deliveryId, authentication)")
    @GetMapping("/api/deliveries/{deliveryId}/cod")
    public ResponseEntity<CodResponse> getCodByDeliveryId(
            @Parameter(description = "Delivery ID", required = true)
            @PathVariable Long deliveryId,
            Authentication authentication
    ) {
        CodResponse response = cashOnDeliveryService.getCodByDeliveryId(deliveryId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Collect COD amount", description = "Records physical cash collection by driver upon delivery. Protected by Idempotency-Key header.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "COD successfully collected"),
            @ApiResponse(responseCode = "400", description = "Invalid collected amount or state violation"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires assigned driver or staff"),
            @ApiResponse(responseCode = "404", description = "COD record not found"),
            @ApiResponse(responseCode = "409", description = "Conflict - concurrent idempotent collection in progress")
    })
    @PreAuthorize("@codSecurity.canCollect(#codId, authentication)")
    @PatchMapping("/api/cod/{codId}/collect")
    public ResponseEntity<CodResponse> collectCod(
            @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, description = "Unique idempotency key (UUID) to prevent duplicate collections", required = false)
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Parameter(description = "COD ID", required = true)
            @PathVariable Long codId,
            @Valid @RequestBody CollectCodRequest request,
            Authentication authentication
    ) {
        String requestHash = idempotencyHashService.hashCodCollection(codId, request);
        CodResponse response = idempotencyService.execute(
                idempotencyKey,
                "COLLECT_COD",
                requestHash,
                authentication != null ? authentication.getName() : null,
                CodResponse.class,
                HttpStatus.OK.value(),
                () -> cashOnDeliveryService.collectCod(
                        codId,
                        request.collectedAmount(),
                        request.notes(),
                        authentication
                )
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Mark COD collection as failed", description = "Records a failure in collecting cash on delivery.")
    @PreAuthorize("@codSecurity.canManage(#codId, authentication)")
    @PatchMapping("/api/cod/{codId}/fail")
    public ResponseEntity<CodResponse> markAsFailed(
            @Parameter(description = "COD ID", required = true)
            @PathVariable Long codId,
            @RequestBody(required = false) FailCodRequest request
    ) {
        String notes = request != null ? request.notes() : null;
        CodResponse response = cashOnDeliveryService.markAsFailed(codId, notes);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cancel COD order", description = "Cancels a cash on delivery order.")
    @PreAuthorize("@codSecurity.canManage(#codId, authentication)")
    @PatchMapping("/api/cod/{codId}/cancel")
    public ResponseEntity<CodResponse> cancelCod(
            @Parameter(description = "COD ID", required = true)
            @PathVariable Long codId
    ) {
        CodResponse response = cashOnDeliveryService.cancelCod(codId);
        return ResponseEntity.ok(response);
    }
}
