package com.ahmed.logistics.shipment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TrackingNumberGeneratorTest {

    private final TrackingNumberGenerator generator = new TrackingNumberGenerator();

    @Test
    @DisplayName("generate produces non-null tracking number with SHP- prefix and 12 chars length")
    void generate_producesValidTrackingNumber() {
        String trackingNumber = generator.generate();

        assertNotNull(trackingNumber);
        assertTrue(trackingNumber.startsWith("SHP-"));
        assertEquals(12, trackingNumber.length()); // "SHP-" + 8 chars
    }

    @Test
    @DisplayName("generate produces different tracking numbers on subsequent invocations")
    void generate_producesDistinctValues() {
        String t1 = generator.generate();
        String t2 = generator.generate();

        assertNotEquals(t1, t2);
    }
}
