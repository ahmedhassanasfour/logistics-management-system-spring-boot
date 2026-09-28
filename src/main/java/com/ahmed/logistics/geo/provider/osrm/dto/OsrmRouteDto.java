package com.ahmed.logistics.geo.provider.osrm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OsrmRouteDto(
        @JsonProperty("distance") Double distance,
        @JsonProperty("duration") Double duration,
        @JsonProperty("weight") Double weight
) {}
