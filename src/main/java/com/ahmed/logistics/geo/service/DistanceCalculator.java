package com.ahmed.logistics.geo.service;

import com.ahmed.logistics.exception.DistanceCalculationException;
import com.ahmed.logistics.geo.model.DistanceResult;
import com.ahmed.logistics.geo.model.GeoLocation;

import java.math.BigDecimal;

public interface DistanceCalculator {

    /**
     * Calculates the driving route distance and estimated duration between origin and destination coordinates.
     *
     * @param originLat latitude of origin
     * @param originLon longitude of origin
     * @param destLat latitude of destination
     * @param destLon longitude of destination
     * @return DistanceResult containing distance in kilometers and duration in minutes
     * @throws DistanceCalculationException if calculation fails or coordinates are invalid
     */
    DistanceResult calculateDistance(BigDecimal originLat, BigDecimal originLon,
                                     BigDecimal destLat, BigDecimal destLon);

    /**
     * Overloaded convenience method accepting GeoLocation objects.
     *
     * @param origin origin location
     * @param destination destination location
     * @return DistanceResult containing distance in kilometers and duration in minutes
     * @throws DistanceCalculationException if calculation fails or locations are null
     */
    default DistanceResult calculateDistance(GeoLocation origin, GeoLocation destination) {
        if (origin == null) {
            throw new DistanceCalculationException("Origin location must not be null");
        }
        if (destination == null) {
            throw new DistanceCalculationException("Destination location must not be null");
        }
        return calculateDistance(origin.latitude(), origin.longitude(), destination.latitude(), destination.longitude());
    }
}
