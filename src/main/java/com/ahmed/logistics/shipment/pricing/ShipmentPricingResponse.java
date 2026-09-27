package com.ahmed.logistics.shipment.pricing;

import java.math.BigDecimal;

public record ShipmentPricingResponse(
        BigDecimal basePrice,
        BigDecimal shippingFee,
        BigDecimal totalPrice
) {}
