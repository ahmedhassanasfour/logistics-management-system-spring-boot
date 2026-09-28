package com.ahmed.logistics.payment.cod.dto;

import jakarta.validation.constraints.Size;

public record CreateCodRequest(
        Long deliveryId,

        @Size(max = 1000, message = "Notes must not exceed 1000 characters")
        String notes
) {
    public CreateCodRequest(Long deliveryId) {
        this(deliveryId, null);
    }
}
