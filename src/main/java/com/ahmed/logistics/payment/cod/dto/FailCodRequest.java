package com.ahmed.logistics.payment.cod.dto;

import jakarta.validation.constraints.Size;

public record FailCodRequest(
        @Size(max = 1000, message = "Notes must not exceed 1000 characters")
        String notes
) {}
