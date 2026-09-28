package com.ahmed.logistics.delivery.pod.controller;

import com.ahmed.logistics.delivery.pod.dto.CreateProofOfDeliveryRequest;
import com.ahmed.logistics.delivery.pod.dto.ProofOfDeliveryResponse;
import com.ahmed.logistics.delivery.pod.service.ProofOfDeliveryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Proof of Delivery", description = "Proof of delivery (POD) records, recipient signatures, and delivery completion confirmation")
@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class ProofOfDeliveryController {

    private final ProofOfDeliveryService proofOfDeliveryService;

    @PreAuthorize("@proofOfDeliverySecurity.canCreate(#deliveryId, authentication)")
    @PostMapping("/{deliveryId}/pod")
    public ResponseEntity<ProofOfDeliveryResponse> createProofOfDelivery(
            @PathVariable Long deliveryId,
            @Valid @RequestBody CreateProofOfDeliveryRequest request,
            Authentication authentication
    ) {
        ProofOfDeliveryResponse response = proofOfDeliveryService.createProofOfDelivery(deliveryId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("@proofOfDeliverySecurity.canRead(#deliveryId, authentication)")
    @GetMapping("/{deliveryId}/pod")
    public ResponseEntity<ProofOfDeliveryResponse> getProofOfDelivery(
            @PathVariable Long deliveryId,
            Authentication authentication
    ) {
        ProofOfDeliveryResponse response = proofOfDeliveryService.getProofOfDelivery(deliveryId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@proofOfDeliverySecurity.canReadByShipment(#shipmentId, authentication)")
    @GetMapping("/shipment/{shipmentId}/pod")
    public ResponseEntity<ProofOfDeliveryResponse> getProofOfDeliveryByShipment(
            @PathVariable Long shipmentId,
            Authentication authentication
    ) {
        ProofOfDeliveryResponse response = proofOfDeliveryService.getProofOfDeliveryByShipment(shipmentId);
        return ResponseEntity.ok(response);
    }
}
