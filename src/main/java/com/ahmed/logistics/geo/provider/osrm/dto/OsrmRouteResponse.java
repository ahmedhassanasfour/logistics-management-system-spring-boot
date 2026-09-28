package com.ahmed.logistics.geo.provider.osrm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OsrmRouteResponse(
        @JsonProperty("code") String code,
        @JsonProperty("message") String message,
        @JsonProperty("routes") List<OsrmRouteDto> routes
) {}
