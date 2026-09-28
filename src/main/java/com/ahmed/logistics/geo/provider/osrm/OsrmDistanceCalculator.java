package com.ahmed.logistics.geo.provider.osrm;

import com.ahmed.logistics.exception.DistanceCalculationException;
import com.ahmed.logistics.geo.model.DistanceResult;
import com.ahmed.logistics.geo.provider.osrm.dto.OsrmRouteDto;
import com.ahmed.logistics.geo.provider.osrm.dto.OsrmRouteResponse;
import com.ahmed.logistics.geo.service.DistanceCalculator;
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
import java.math.RoundingMode;
import java.time.Duration;

@Slf4j
@Service
public class OsrmDistanceCalculator implements DistanceCalculator {

    private final RestClient restClient;
    private final String baseUrl;

    @Autowired
    public OsrmDistanceCalculator(
            @Value("${app.geo.osrm.base-url:https://router.project-osrm.org}") String baseUrl,
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
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public OsrmDistanceCalculator(RestClient restClient, String baseUrl) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
    }

    @Override
    public DistanceResult calculateDistance(BigDecimal originLat, BigDecimal originLon,
                                             BigDecimal destLat, BigDecimal destLon) {
        validateCoordinates(originLat, originLon, destLat, destLon);

        long start = System.currentTimeMillis();
        log.info("OSRM Routing: calculating distance from [{}, {}] to [{}, {}]",
                originLat, originLon, destLat, destLon);

        String path = String.format("/route/v1/driving/%s,%s;%s,%s",
                originLon.toPlainString(), originLat.toPlainString(),
                destLon.toPlainString(), destLat.toPlainString());

        try {
            OsrmRouteResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(path)
                            .queryParam("overview", "false")
                            .queryParam("alternatives", "false")
                            .queryParam("steps", "false")
                            .build())
                    .retrieve()
                    .body(OsrmRouteResponse.class);

            if (response == null) {
                throw new DistanceCalculationException("Empty response received from OSRM routing service");
            }

            if (!"Ok".equalsIgnoreCase(response.code())) {
                String msg = response.message() != null ? response.message() : response.code();
                log.warn("OSRM Routing returned non-Ok code: {} (message: {})", response.code(), msg);
                throw new DistanceCalculationException("OSRM routing service failed to find route: " + msg);
            }

            if (response.routes() == null || response.routes().isEmpty() || response.routes().get(0) == null) {
                log.warn("OSRM Routing returned no routes for coordinates [{}, {}] -> [{}, {}]",
                        originLat, originLon, destLat, destLon);
                throw new DistanceCalculationException("No routes returned by OSRM routing service");
            }

            OsrmRouteDto route = response.routes().get(0);
            if (route.distance() == null || route.duration() == null) {
                throw new DistanceCalculationException("OSRM route did not contain valid distance or duration");
            }

            BigDecimal distanceKm = BigDecimal.valueOf(route.distance())
                    .divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
            BigDecimal durationMinutes = BigDecimal.valueOf(route.duration())
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

            DistanceResult result = new DistanceResult(distanceKm, durationMinutes);
            long elapsed = System.currentTimeMillis() - start;
            log.info("OSRM Routing success: [{}, {}] to [{}, {}] -> distance={} km, duration={} min in {}ms",
                    originLat, originLon, destLat, destLon, result.distanceKm(), result.durationMinutes(), elapsed);

            return result;

        } catch (DistanceCalculationException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("OSRM Routing network/timeout failure from [{}, {}] to [{}, {}] after {}ms: {}",
                    originLat, originLon, destLat, destLon, elapsed, ex.getMessage());
            throw new DistanceCalculationException("Distance routing service unavailable or timed out: " + ex.getMessage(), ex);
        } catch (RestClientResponseException ex) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("OSRM Routing HTTP error {} from [{}, {}] to [{}, {}] after {}ms: {}",
                    ex.getStatusCode(), originLat, originLon, destLat, destLon, elapsed, ex.getMessage());
            throw new DistanceCalculationException("Distance routing service returned HTTP error " + ex.getStatusCode(), ex);
        } catch (Exception ex) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("OSRM Routing unexpected failure from [{}, {}] to [{}, {}] after {}ms: {}",
                    originLat, originLon, destLat, destLon, elapsed, ex.getMessage(), ex);
            throw new DistanceCalculationException("Unexpected error during distance calculation: " + ex.getMessage(), ex);
        }
    }

    private void validateCoordinates(BigDecimal originLat, BigDecimal originLon,
                                     BigDecimal destLat, BigDecimal destLon) {
        if (originLat == null || originLon == null) {
            throw new DistanceCalculationException("Origin latitude and longitude must not be null");
        }
        if (destLat == null || destLon == null) {
            throw new DistanceCalculationException("Destination latitude and longitude must not be null");
        }
        if (originLat.compareTo(BigDecimal.valueOf(-90)) < 0 || originLat.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new DistanceCalculationException(String.format("Invalid origin latitude: %s. Must be between -90 and +90", originLat));
        }
        if (originLon.compareTo(BigDecimal.valueOf(-180)) < 0 || originLon.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new DistanceCalculationException(String.format("Invalid origin longitude: %s. Must be between -180 and +180", originLon));
        }
        if (destLat.compareTo(BigDecimal.valueOf(-90)) < 0 || destLat.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new DistanceCalculationException(String.format("Invalid destination latitude: %s. Must be between -90 and +90", destLat));
        }
        if (destLon.compareTo(BigDecimal.valueOf(-180)) < 0 || destLon.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new DistanceCalculationException(String.format("Invalid destination longitude: %s. Must be between -180 and +180", destLon));
        }
    }
}
