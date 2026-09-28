package com.ahmed.logistics.geo.model;

import com.ahmed.logistics.exception.BadRequestException;

import java.math.BigDecimal;

public record GeoLocation(
        BigDecimal latitude,
        BigDecimal longitude
) {
    public GeoLocation {
        if (latitude == null || longitude == null) {
            throw new BadRequestException("Latitude and longitude must not be null");
        }
        if (latitude.compareTo(BigDecimal.valueOf(-90)) < 0 || latitude.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new BadRequestException(String.format("Invalid latitude: %s. Must be between -90 and +90", latitude));
        }
        if (longitude.compareTo(BigDecimal.valueOf(-180)) < 0 || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new BadRequestException(String.format("Invalid longitude: %s. Must be between -180 and +180", longitude));
        }
    }

    public static GeoLocation of(double latitude, double longitude) {
        return new GeoLocation(BigDecimal.valueOf(latitude), BigDecimal.valueOf(longitude));
    }
}
