package com.ahmed.logistics.geo.provider.nominatim;

import com.ahmed.logistics.exception.GeocodingException;
import com.ahmed.logistics.geo.model.GeoLocation;
import com.ahmed.logistics.geo.provider.nominatim.dto.NominatimPlaceDto;
import com.ahmed.logistics.geo.service.GeocodingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.Duration;

@Slf4j
@Service
public class NominatimGeocodingService implements GeocodingService {

    private final RestClient restClient;
    private final String baseUrl;

    @Autowired
    public NominatimGeocodingService(
            @Value("${app.geo.nominatim.base-url:https://nominatim.openstreetmap.org}") String baseUrl,
            @Value("${app.geo.nominatim.user-agent:AhmedLogistics/1.0}") String userAgent,
            @Value("${app.geo.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${app.geo.read-timeout-ms:5000}") int readTimeoutMs
    ) {
        this.baseUrl = baseUrl;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public NominatimGeocodingService(RestClient restClient, String baseUrl) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
    }

    @Override
    public GeoLocation geocode(String address) {
        if (address == null || address.isBlank()) {
            throw new GeocodingException("Address for geocoding must not be null or blank");
        }

        String trimmedAddress = address.trim();
        long start = System.currentTimeMillis();
        log.info("Nominatim Geocoding: requesting coordinates for address '{}'", trimmedAddress);

        try {
            NominatimPlaceDto[] results = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("q", trimmedAddress)
                            .queryParam("format", "json")
                            .queryParam("limit", "1")
                            .build())
                    .retrieve()
                    .body(NominatimPlaceDto[].class);

            if (results == null || results.length == 0 || results[0] == null) {
                long elapsed = System.currentTimeMillis() - start;
                log.warn("Nominatim Geocoding: no results found for address '{}' (elapsed: {}ms)", trimmedAddress, elapsed);
                throw new GeocodingException("No geocoding results found for address: " + trimmedAddress);
            }

            NominatimPlaceDto firstResult = results[0];
            if (firstResult.lat() == null || firstResult.lon() == null) {
                throw new GeocodingException("Geocoding result missing latitude/longitude for address: " + trimmedAddress);
            }

            BigDecimal latitude;
            BigDecimal longitude;
            try {
                latitude = new BigDecimal(firstResult.lat().trim());
                longitude = new BigDecimal(firstResult.lon().trim());
            } catch (NumberFormatException e) {
                throw new GeocodingException("Invalid coordinate format received from Nominatim: lat="
                        + firstResult.lat() + ", lon=" + firstResult.lon(), e);
            }

            GeoLocation geoLocation = new GeoLocation(latitude, longitude);
            long elapsed = System.currentTimeMillis() - start;
            log.info("Nominatim Geocoding success: address '{}' -> [lat={}, lon={}] in {}ms",
                    trimmedAddress, geoLocation.latitude(), geoLocation.longitude(), elapsed);

            return geoLocation;

        } catch (GeocodingException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("Nominatim Geocoding network/timeout failure for address '{}' after {}ms: {}",
                    trimmedAddress, elapsed, ex.getMessage());
            throw new GeocodingException("Geocoding service unavailable or timed out: " + ex.getMessage(), ex);
        } catch (RestClientResponseException ex) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("Nominatim Geocoding HTTP error {} for address '{}' after {}ms: {}",
                    ex.getStatusCode(), trimmedAddress, elapsed, ex.getMessage());
            throw new GeocodingException("Geocoding service returned HTTP error " + ex.getStatusCode(), ex);
        } catch (Exception ex) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("Nominatim Geocoding unexpected failure for address '{}' after {}ms: {}",
                    trimmedAddress, elapsed, ex.getMessage(), ex);
            throw new GeocodingException("Unexpected error during geocoding: " + ex.getMessage(), ex);
        }
    }
}
