package com.ahmed.logistics.payment.cod.controller;

import com.ahmed.logistics.payment.cod.dto.CollectCodRequest;
import com.ahmed.logistics.payment.cod.dto.CreateCodRequest;
import com.ahmed.logistics.payment.cod.dto.FailCodRequest;
import com.ahmed.logistics.payment.cod.dto.CodResponse;
import com.ahmed.logistics.payment.cod.service.CashOnDeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CodController {

    private final CashOnDeliveryService cashOnDeliveryService;

    @PreAuthorize("@codSecurity.canCreate(#deliveryId, authentication)")
    @PostMapping("/api/deliveries/{deliveryId}/cod")
    public ResponseEntity<CodResponse> createCod(
            @PathVariable Long deliveryId,
            @Valid @RequestBody(required = false) CreateCodRequest request
    ) {
        CodResponse response = cashOnDeliveryService.createCod(deliveryId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("@codSecurity.canRead(#codId, authentication)")
    @GetMapping("/api/cod/{codId}")
    public ResponseEntity<CodResponse> getCodById(
            @PathVariable Long codId,
            Authentication authentication
    ) {
        CodResponse response = cashOnDeliveryService.getCodById(codId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@codSecurity.canReadByDelivery(#deliveryId, authentication)")
    @GetMapping("/api/deliveries/{deliveryId}/cod")
    public ResponseEntity<CodResponse> getCodByDeliveryId(
            @PathVariable Long deliveryId,
            Authentication authentication
    ) {
        CodResponse response = cashOnDeliveryService.getCodByDeliveryId(deliveryId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@codSecurity.canCollect(#codId, authentication)")
    @PatchMapping("/api/cod/{codId}/collect")
    public ResponseEntity<CodResponse> collectCod(
            @PathVariable Long codId,
            @Valid @RequestBody CollectCodRequest request,
            Authentication authentication
    ) {
        CodResponse response = cashOnDeliveryService.collectCod(
                codId,
                request.collectedAmount(),
                request.notes(),
                authentication
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@codSecurity.canManage(#codId, authentication)")
    @PatchMapping("/api/cod/{codId}/fail")
    public ResponseEntity<CodResponse> markAsFailed(
            @PathVariable Long codId,
            @RequestBody(required = false) FailCodRequest request
    ) {
        String notes = request != null ? request.notes() : null;
        CodResponse response = cashOnDeliveryService.markAsFailed(codId, notes);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@codSecurity.canManage(#codId, authentication)")
    @PatchMapping("/api/cod/{codId}/cancel")
    public ResponseEntity<CodResponse> cancelCod(
            @PathVariable Long codId
    ) {
        CodResponse response = cashOnDeliveryService.cancelCod(codId);
        return ResponseEntity.ok(response);
    }
}
