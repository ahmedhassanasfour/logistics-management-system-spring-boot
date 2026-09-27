package com.ahmed.logistics.shipment.pricing;

import com.ahmed.logistics.exception.BadRequestException;
import com.ahmed.logistics.shipment.entity.ShipmentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
public class ShipmentPricingService {

    public static final BigDecimal BASE_INCLUDED_WEIGHT_KG = new BigDecimal("1.00");

    public static final BigDecimal STANDARD_BASE_PRICE = new BigDecimal("15.00");
    public static final BigDecimal EXPRESS_BASE_PRICE = new BigDecimal("25.00");
    public static final BigDecimal SAME_DAY_BASE_PRICE = new BigDecimal("40.00");

    public static final BigDecimal STANDARD_ADDITIONAL_KG_RATE = new BigDecimal("2.00");
    public static final BigDecimal EXPRESS_ADDITIONAL_KG_RATE = new BigDecimal("3.00");
    public static final BigDecimal SAME_DAY_ADDITIONAL_KG_RATE = new BigDecimal("5.00");

    public ShipmentPricingResponse calculatePrice(ShipmentType shipmentType, BigDecimal weight) {
        validateInputs(shipmentType, weight);

        BigDecimal basePrice = getBasePrice(shipmentType);
        BigDecimal ratePerKg = getRatePerAdditionalKg(shipmentType);

        BigDecimal shippingFee;
        if (weight.compareTo(BASE_INCLUDED_WEIGHT_KG) <= 0) {
            shippingFee = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            BigDecimal additionalWeight = weight.subtract(BASE_INCLUDED_WEIGHT_KG);
            shippingFee = additionalWeight.multiply(ratePerKg).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal totalPrice = basePrice.add(shippingFee).setScale(2, RoundingMode.HALF_UP);

        log.debug("Calculated price for type: {}, weight: {} kg -> basePrice: {}, shippingFee: {}, totalPrice: {}",
                shipmentType, weight, basePrice, shippingFee, totalPrice);

        return new ShipmentPricingResponse(
                basePrice.setScale(2, RoundingMode.HALF_UP),
                shippingFee,
                totalPrice
        );
    }

    public ShipmentPricingResponse calculatePrice(ShipmentType shipmentType, Double weightKg) {
        if (weightKg == null) {
            log.warn("Pricing calculation failed: Weight is null");
            throw new BadRequestException("Shipment weight is required for pricing calculation");
        }
        return calculatePrice(shipmentType, BigDecimal.valueOf(weightKg));
    }

    private void validateInputs(ShipmentType shipmentType, BigDecimal weight) {
        if (shipmentType == null) {
            log.warn("Pricing calculation failed: Shipment type is null");
            throw new BadRequestException("Shipment type is required for pricing calculation");
        }
        if (weight == null) {
            log.warn("Pricing calculation failed: Weight is null");
            throw new BadRequestException("Shipment weight is required for pricing calculation");
        }
        if (weight.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Pricing calculation failed: Weight {} is not greater than zero", weight);
            throw new BadRequestException("Shipment weight must be greater than zero");
        }
    }

    private BigDecimal getBasePrice(ShipmentType type) {
        return switch (type) {
            case STANDARD -> STANDARD_BASE_PRICE;
            case EXPRESS -> EXPRESS_BASE_PRICE;
            case SAME_DAY -> SAME_DAY_BASE_PRICE;
        };
    }

    private BigDecimal getRatePerAdditionalKg(ShipmentType type) {
        return switch (type) {
            case STANDARD -> STANDARD_ADDITIONAL_KG_RATE;
            case EXPRESS -> EXPRESS_ADDITIONAL_KG_RATE;
            case SAME_DAY -> SAME_DAY_ADDITIONAL_KG_RATE;
        };
    }
}
