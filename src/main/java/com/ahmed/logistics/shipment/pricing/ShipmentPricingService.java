package com.ahmed.logistics.shipment.pricing;

import com.ahmed.logistics.exception.BadRequestException;
import com.ahmed.logistics.geo.model.DistanceResult;
import com.ahmed.logistics.geo.model.GeoLocation;
import com.ahmed.logistics.geo.service.DistanceCalculator;
import com.ahmed.logistics.geo.service.GeocodingService;
import com.ahmed.logistics.shipment.entity.ShipmentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    private final GeocodingService geocodingService;
    private final DistanceCalculator distanceCalculator;
    private final BigDecimal distanceRatePerKm;

    @Autowired
    public ShipmentPricingService(
            GeocodingService geocodingService,
            DistanceCalculator distanceCalculator,
            @Value("${app.pricing.distance-rate-per-km:1.50}") BigDecimal distanceRatePerKm
    ) {
        this.geocodingService = geocodingService;
        this.distanceCalculator = distanceCalculator;
        this.distanceRatePerKm = distanceRatePerKm != null ? distanceRatePerKm : new BigDecimal("1.50");
    }

    public ShipmentPricingService() {
        this(null, null, new BigDecimal("1.50"));
    }

    public ShipmentPricingResponse calculatePrice(
            ShipmentType shipmentType,
            BigDecimal weight,
            String pickupAddress,
            String deliveryAddress
    ) {
        validateInputs(shipmentType, weight);
        validateAddresses(pickupAddress, deliveryAddress);

        if (geocodingService == null || distanceCalculator == null) {
            throw new IllegalStateException("GeocodingService and DistanceCalculator must be configured for distance-based pricing");
        }

        BigDecimal basePrice = getBasePrice(shipmentType);
        BigDecimal shippingFee = calculateWeightCharge(shipmentType, weight);

        // Server-side distance calculation flow:
        // pickupAddress -> Nominatim -> pickup coordinates
        // deliveryAddress -> Nominatim -> delivery coordinates
        // coordinates -> OSRM -> distanceKm
        GeoLocation pickupLocation = geocodingService.geocode(pickupAddress);
        GeoLocation deliveryLocation = geocodingService.geocode(deliveryAddress);

        DistanceResult distanceResult = distanceCalculator.calculateDistance(pickupLocation, deliveryLocation);
        BigDecimal distanceKm = distanceResult.distanceKm().setScale(2, RoundingMode.HALF_UP);

        // distanceCharge = distanceKm * distanceRatePerKm
        BigDecimal distanceCharge = distanceKm.multiply(distanceRatePerKm).setScale(2, RoundingMode.HALF_UP);

        // finalPrice = basePrice + weightCharge + distanceCharge
        BigDecimal totalPrice = basePrice.add(shippingFee).add(distanceCharge).setScale(2, RoundingMode.HALF_UP);

        log.info("Calculated distance pricing for type={}, weight={} kg, distance={} km -> basePrice={}, weightCharge={}, distanceCharge={}, totalPrice={}",
                shipmentType, weight, distanceKm, basePrice, shippingFee, distanceCharge, totalPrice);

        return new ShipmentPricingResponse(
                basePrice.setScale(2, RoundingMode.HALF_UP),
                shippingFee,
                distanceKm,
                distanceCharge,
                totalPrice
        );
    }

    public ShipmentPricingResponse calculatePrice(
            ShipmentType shipmentType,
            Double weightKg,
            String pickupAddress,
            String deliveryAddress
    ) {
        if (weightKg == null) {
            log.warn("Pricing calculation failed: Weight is null");
            throw new BadRequestException("Shipment weight is required for pricing calculation");
        }
        return calculatePrice(shipmentType, BigDecimal.valueOf(weightKg), pickupAddress, deliveryAddress);
    }

    public ShipmentPricingResponse calculatePrice(ShipmentType shipmentType, BigDecimal weight) {
        validateInputs(shipmentType, weight);

        BigDecimal basePrice = getBasePrice(shipmentType);
        BigDecimal shippingFee = calculateWeightCharge(shipmentType, weight);
        BigDecimal totalPrice = basePrice.add(shippingFee).setScale(2, RoundingMode.HALF_UP);

        log.debug("Calculated price without distance for type: {}, weight: {} kg -> basePrice: {}, shippingFee: {}, totalPrice: {}",
                shipmentType, weight, basePrice, shippingFee, totalPrice);

        return new ShipmentPricingResponse(
                basePrice.setScale(2, RoundingMode.HALF_UP),
                shippingFee,
                null,
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
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

    private void validateAddresses(String pickupAddress, String deliveryAddress) {
        if (pickupAddress == null || pickupAddress.isBlank()) {
            log.warn("Pricing calculation failed: Pickup address is blank");
            throw new BadRequestException("Pickup address is required for pricing calculation");
        }
        if (deliveryAddress == null || deliveryAddress.isBlank()) {
            log.warn("Pricing calculation failed: Delivery address is blank");
            throw new BadRequestException("Delivery address is required for pricing calculation");
        }
    }

    private BigDecimal calculateWeightCharge(ShipmentType shipmentType, BigDecimal weight) {
        BigDecimal ratePerKg = getRatePerAdditionalKg(shipmentType);
        if (weight.compareTo(BASE_INCLUDED_WEIGHT_KG) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            BigDecimal additionalWeight = weight.subtract(BASE_INCLUDED_WEIGHT_KG);
            return additionalWeight.multiply(ratePerKg).setScale(2, RoundingMode.HALF_UP);
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

    public BigDecimal getDistanceRatePerKm() {
        return distanceRatePerKm;
    }
}
