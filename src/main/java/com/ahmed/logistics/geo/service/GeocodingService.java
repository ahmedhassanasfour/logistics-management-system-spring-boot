package com.ahmed.logistics.geo.service;

import com.ahmed.logistics.geo.model.GeoLocation;

public interface GeocodingService {

    /**
     * Converts a textual address into geographic coordinates.
     *
     * @param address textual address to geocode
     * @return GeoLocation containing latitude and longitude
     * @throws com.ahmed.logistics.exception.GeocodingException if geocoding fails or no result found
     */
    GeoLocation geocode(String address);
}
