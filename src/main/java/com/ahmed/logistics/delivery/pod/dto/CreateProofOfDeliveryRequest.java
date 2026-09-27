package com.ahmed.logistics.delivery.pod.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProofOfDeliveryRequest(
        @NotBlank(message = "Recipient name is required")
        @Size(max = 100, message = "Recipient name must not exceed 100 characters")
        String recipientName,

        @NotBlank(message = "Recipient phone is required")
        @Size(max = 30, message = "Recipient phone must not exceed 30 characters")
        String recipientPhone,

        @Size(max = 50, message = "Recipient ID must not exceed 50 characters")
        String recipientId,

        @Size(max = 500, message = "Notes must not exceed 500 characters")
        String notes
) {}
