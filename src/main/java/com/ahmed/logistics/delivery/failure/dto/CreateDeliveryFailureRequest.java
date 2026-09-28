package com.ahmed.logistics.delivery.failure.dto;

import com.ahmed.logistics.delivery.failure.entity.DeliveryFailureReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateDeliveryFailureRequest(
        @NotNull(message = "Failure reason is required")
        DeliveryFailureReason reason,

        @Size(max = 500, message = "Notes must not exceed 500 characters")
        String notes
) {}
