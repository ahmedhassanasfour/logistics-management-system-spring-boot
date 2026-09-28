package com.ahmed.logistics.geo.provider.nominatim.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NominatimPlaceDto(
        @JsonProperty("lat") String lat,
        @JsonProperty("lon") String lon,
        @JsonProperty("display_name") String displayName,
        @JsonProperty("type") String type,
        @JsonProperty("importance") Double importance
) {}
