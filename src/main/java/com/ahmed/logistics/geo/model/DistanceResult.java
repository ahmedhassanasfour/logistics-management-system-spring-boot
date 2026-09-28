package com.ahmed.logistics.geo.model;

import com.ahmed.logistics.exception.BadRequestException;

import java.math.BigDecimal;

public record DistanceResult(
        BigDecimal distanceKm,
        BigDecimal durationMinutes
) {
    public DistanceResult {
        if (distanceKm == null || durationMinutes == null) {
            throw new BadRequestException("Distance (km) and duration (minutes) must not be null");
        }
    }
}
