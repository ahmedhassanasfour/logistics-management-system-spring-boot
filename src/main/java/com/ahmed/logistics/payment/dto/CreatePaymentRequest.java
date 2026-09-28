package com.ahmed.logistics.payment.dto;

import com.ahmed.logistics.payment.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record CreatePaymentRequest(
        @NotNull(message = "Payment method is required")
        PaymentMethod method
) {}
