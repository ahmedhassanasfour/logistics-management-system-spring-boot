package com.ahmed.logistics.event;

import java.math.BigDecimal;

public record CodCollectedEvent(
        Long codId,
        Long shipmentId,
        Long customerId,
        BigDecimal amount
) {}
