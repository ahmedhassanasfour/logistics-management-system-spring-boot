package com.ahmed.logistics.payment.cod.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CollectCodRequest(
        @NotNull(message = "Collected amount is required")
        @DecimalMin(value = "0.01", message = "Collected amount must be greater than zero")
        BigDecimal collectedAmount,

        @Size(max = 1000, message = "Notes must not exceed 1000 characters")
        String notes
) {}
