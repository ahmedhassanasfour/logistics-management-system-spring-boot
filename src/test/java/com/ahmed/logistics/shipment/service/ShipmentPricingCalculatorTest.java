package com.ahmed.logistics.shipment.service;

import com.ahmed.logistics.shipment.entity.ShipmentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ShipmentPricingCalculatorTest {

    private final ShipmentPricingCalculator calculator = new ShipmentPricingCalculator();

    @Test
    @DisplayName("calculate computes correct pricing for STANDARD shipment")
    void calculate_standardShipment() {
        ShipmentPricingCalculator.PriceBreakdown result = calculator.calculate(ShipmentType.STANDARD, 10.0);

        assertEquals(new BigDecimal("15.00"), result.basePrice());
        assertEquals(new BigDecimal("25.00"), result.shippingFee()); // 10.0 * 2.50 = 25.00
        assertEquals(new BigDecimal("40.00"), result.totalPrice());  // 15.00 + 25.00 = 40.00
    }

    @Test
    @DisplayName("calculate computes correct pricing for EXPRESS shipment")
    void calculate_expressShipment() {
        ShipmentPricingCalculator.PriceBreakdown result = calculator.calculate(ShipmentType.EXPRESS, 5.0);

        assertEquals(new BigDecimal("25.00"), result.basePrice());
        assertEquals(new BigDecimal("25.00"), result.shippingFee()); // 5.0 * 5.00 = 25.00
        assertEquals(new BigDecimal("50.00"), result.totalPrice());  // 25.00 + 25.00 = 50.00
    }

    @Test
    @DisplayName("calculate computes correct pricing for SAME_DAY shipment")
    void calculate_sameDayShipment() {
        ShipmentPricingCalculator.PriceBreakdown result = calculator.calculate(ShipmentType.SAME_DAY, 2.0);

        assertEquals(new BigDecimal("40.00"), result.basePrice());
        assertEquals(new BigDecimal("16.00"), result.shippingFee()); // 2.0 * 8.00 = 16.00
        assertEquals(new BigDecimal("56.00"), result.totalPrice());  // 40.00 + 16.00 = 56.00
    }
}
