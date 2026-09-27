package com.ahmed.logistics.shipment.service;

import com.ahmed.logistics.shipment.entity.ShipmentType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class ShipmentPricingCalculator {

    public record PriceBreakdown(
            BigDecimal basePrice,
            BigDecimal shippingFee,
            BigDecimal totalPrice
    ) {}

    public PriceBreakdown calculate(ShipmentType type, Double weightKg) {
        BigDecimal base = switch (type) {
            case STANDARD -> new BigDecimal("15.00");
            case EXPRESS -> new BigDecimal("25.00");
            case SAME_DAY -> new BigDecimal("40.00");
        };

        double ratePerKg = switch (type) {
            case STANDARD -> 2.50;
            case EXPRESS -> 5.00;
            case SAME_DAY -> 8.00;
        };

        double weight = (weightKg != null && weightKg > 0) ? weightKg : 1.0;
        BigDecimal fee = BigDecimal.valueOf(weight * ratePerKg).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = base.add(fee).setScale(2, RoundingMode.HALF_UP);

        return new PriceBreakdown(base, fee, total);
    }
}
