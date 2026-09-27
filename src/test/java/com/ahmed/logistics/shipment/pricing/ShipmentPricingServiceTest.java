package com.ahmed.logistics.shipment.pricing;

import com.ahmed.logistics.exception.BadRequestException;
import com.ahmed.logistics.shipment.entity.ShipmentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ShipmentPricingServiceTest {

    private final ShipmentPricingService pricingService = new ShipmentPricingService();

    @Test
    @DisplayName("calculatePrice for STANDARD shipment with 1 kg has base price and zero fee")
    void calculatePrice_standard_oneKg() {
        ShipmentPricingResponse response = pricingService.calculatePrice(ShipmentType.STANDARD, new BigDecimal("1.00"));

        assertEquals(new BigDecimal("15.00"), response.basePrice());
        assertEquals(new BigDecimal("0.00"), response.shippingFee());
        assertEquals(new BigDecimal("15.00"), response.totalPrice());
    }

    @Test
    @DisplayName("calculatePrice for STANDARD shipment with weight < 1 kg has zero additional fee")
    void calculatePrice_standard_fractionalUnderOneKg() {
        ShipmentPricingResponse response = pricingService.calculatePrice(ShipmentType.STANDARD, new BigDecimal("0.50"));

        assertEquals(new BigDecimal("15.00"), response.basePrice());
        assertEquals(new BigDecimal("0.00"), response.shippingFee());
        assertEquals(new BigDecimal("15.00"), response.totalPrice());
    }

    @Test
    @DisplayName("calculatePrice for STANDARD shipment with 3 kg computes correct additional fee")
    void calculatePrice_standard_threeKg() {
        // additional weight = 2 kg * 2.00 = 4.00; total = 15.00 + 4.00 = 19.00
        ShipmentPricingResponse response = pricingService.calculatePrice(ShipmentType.STANDARD, new BigDecimal("3.00"));

        assertEquals(new BigDecimal("15.00"), response.basePrice());
        assertEquals(new BigDecimal("4.00"), response.shippingFee());
        assertEquals(new BigDecimal("19.00"), response.totalPrice());
    }

    @Test
    @DisplayName("calculatePrice for EXPRESS shipment with 4 kg computes correct additional fee")
    void calculatePrice_express_fourKg() {
        // additional weight = 3 kg * 3.00 = 9.00; total = 25.00 + 9.00 = 34.00
        ShipmentPricingResponse response = pricingService.calculatePrice(ShipmentType.EXPRESS, new BigDecimal("4.00"));

        assertEquals(new BigDecimal("25.00"), response.basePrice());
        assertEquals(new BigDecimal("9.00"), response.shippingFee());
        assertEquals(new BigDecimal("34.00"), response.totalPrice());
    }

    @Test
    @DisplayName("calculatePrice for SAME_DAY shipment with 1 kg has base price and zero fee")
    void calculatePrice_sameDay_oneKg() {
        ShipmentPricingResponse response = pricingService.calculatePrice(ShipmentType.SAME_DAY, new BigDecimal("1.00"));

        assertEquals(new BigDecimal("40.00"), response.basePrice());
        assertEquals(new BigDecimal("0.00"), response.shippingFee());
        assertEquals(new BigDecimal("40.00"), response.totalPrice());
    }

    @Test
    @DisplayName("calculatePrice for SAME_DAY shipment with 2 kg computes correct additional fee")
    void calculatePrice_sameDay_twoKg() {
        // additional weight = 1 kg * 5.00 = 5.00; total = 40.00 + 5.00 = 45.00
        ShipmentPricingResponse response = pricingService.calculatePrice(ShipmentType.SAME_DAY, new BigDecimal("2.00"));

        assertEquals(new BigDecimal("40.00"), response.basePrice());
        assertEquals(new BigDecimal("5.00"), response.shippingFee());
        assertEquals(new BigDecimal("45.00"), response.totalPrice());
    }

    @Test
    @DisplayName("calculatePrice handles fractional weights with proper scale and rounding")
    void calculatePrice_fractionalWeight() {
        // STANDARD 2.5 kg -> additional weight = 1.5 kg * 2.00 = 3.00; total = 18.00
        ShipmentPricingResponse response = pricingService.calculatePrice(ShipmentType.STANDARD, new BigDecimal("2.50"));

        assertEquals(new BigDecimal("15.00"), response.basePrice());
        assertEquals(new BigDecimal("3.00"), response.shippingFee());
        assertEquals(new BigDecimal("18.00"), response.totalPrice());
    }

    @Test
    @DisplayName("calculatePrice with Double overload works correctly")
    void calculatePrice_doubleOverload() {
        ShipmentPricingResponse response = pricingService.calculatePrice(ShipmentType.EXPRESS, 4.0);

        assertEquals(new BigDecimal("25.00"), response.basePrice());
        assertEquals(new BigDecimal("9.00"), response.shippingFee());
        assertEquals(new BigDecimal("34.00"), response.totalPrice());
    }

    @Test
    @DisplayName("calculatePrice throws BadRequestException when shipmentType is null")
    void calculatePrice_nullType_throwsBadRequestException() {
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                pricingService.calculatePrice(null, new BigDecimal("5.00")));

        assertEquals("Shipment type is required for pricing calculation", ex.getMessage());
    }

    @Test
    @DisplayName("calculatePrice throws BadRequestException when weight BigDecimal is null")
    void calculatePrice_nullBigDecimalWeight_throwsBadRequestException() {
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                pricingService.calculatePrice(ShipmentType.STANDARD, (BigDecimal) null));

        assertEquals("Shipment weight is required for pricing calculation", ex.getMessage());
    }

    @Test
    @DisplayName("calculatePrice throws BadRequestException when weight Double is null")
    void calculatePrice_nullDoubleWeight_throwsBadRequestException() {
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                pricingService.calculatePrice(ShipmentType.STANDARD, (Double) null));

        assertEquals("Shipment weight is required for pricing calculation", ex.getMessage());
    }

    @ParameterizedTest(name = "Invalid weight: {0}")
    @ValueSource(strings = {"0.00", "-1.00", "-0.01"})
    @DisplayName("calculatePrice throws BadRequestException when weight is zero or negative")
    void calculatePrice_invalidWeight_throwsBadRequestException(String weightStr) {
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                pricingService.calculatePrice(ShipmentType.STANDARD, new BigDecimal(weightStr)));

        assertEquals("Shipment weight must be greater than zero", ex.getMessage());
    }
}
