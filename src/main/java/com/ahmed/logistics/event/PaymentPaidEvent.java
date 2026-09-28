package com.ahmed.logistics.event;

import java.math.BigDecimal;

public record PaymentPaidEvent(
        Long paymentId,
        Long shipmentId,
        Long customerId,
        BigDecimal amount
) {}
