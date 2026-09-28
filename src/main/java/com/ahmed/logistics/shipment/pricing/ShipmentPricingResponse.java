package com.ahmed.logistics.shipment.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record ShipmentPricingResponse(
        BigDecimal basePrice,
        BigDecimal shippingFee,
        BigDecimal distanceKm,
        BigDecimal distanceCharge,
        BigDecimal totalPrice
) {
    public ShipmentPricingResponse(BigDecimal basePrice, BigDecimal shippingFee, BigDecimal totalPrice) {
        this(
                basePrice,
                shippingFee,
                null,
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                totalPrice
        );
    }

    public BigDecimal weightCharge() {
        return shippingFee;
    }
}
