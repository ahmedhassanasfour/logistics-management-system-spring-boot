package com.ahmed.logistics.auth.jwt;

import com.ahmed.logistics.auth.security.CustomUserDetails;
import com.ahmed.logistics.user.entity.Role;
import com.ahmed.logistics.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;

    // 256-bit test secret (32+ chars)
    private static final String TEST_SECRET = "test-secret-key-for-jwt-signing-must-be-at-least-256-bits-long-12345";
    private static final long ACCESS_EXPIRATION = 60000; // 1 min
    private static final long REFRESH_EXPIRATION = 120000; // 2 min

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, ACCESS_EXPIRATION, REFRESH_EXPIRATION);

        User user = User.builder()
                .id(1L)
                .email("admin@logistics.com")
                .password("hash")
                .firstName("Admin")
                .lastName("User")
                .role(Role.ADMIN)
                .enabled(true)
                .build();

        userDetails = new CustomUserDetails(user);
    }

    @Test
    void generateAccessToken_createsValidJwt() {
        String token = jwtService.generateAccessToken(userDetails);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertFalse(jwtService.isRefreshToken(token));
        assertEquals("admin@logistics.com", jwtService.extractUsername(token));
    }

    @Test
    void extractUsername_extractsCorrectSubject() {
        String token = jwtService.generateAccessToken(userDetails);

        String username = jwtService.extractUsername(token);

        assertEquals("admin@logistics.com", username);
    }

    @Test
    void isTokenValid_returnsTrueForValidTokenAndMatchingUser() {
        String token = jwtService.generateAccessToken(userDetails);

        boolean isValid = jwtService.isTokenValid(token, userDetails);

        assertTrue(isValid);
    }

    @Test
    void isTokenValid_returnsFalseForDifferentUser() {
        String token = jwtService.generateAccessToken(userDetails);

        User otherUser = User.builder()
                .email("other@logistics.com")
                .password("hash")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();
        UserDetails otherDetails = new CustomUserDetails(otherUser);

        boolean isValid = jwtService.isTokenValid(token, otherDetails);

        assertFalse(isValid);
    }

    @Test
    void expiredOrInvalidToken_isRejected() {
        // Instant expiration token service
        JwtService shortLivedJwtService = new JwtService(TEST_SECRET, -1000L, -1000L);
        String expiredToken = shortLivedJwtService.generateAccessToken(userDetails);

        assertTrue(jwtService.isTokenExpired(expiredToken));
        assertFalse(jwtService.isTokenValid(expiredToken, userDetails));

        // Invalid signature / malformed token
        String malformedToken = "invalid.token.structure";
        assertFalse(jwtService.isTokenValid(malformedToken, userDetails));
    }

    @Test
    void generateRefreshToken_createsRefreshToken() {
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        assertNotNull(refreshToken);
        assertTrue(jwtService.isRefreshToken(refreshToken));
        assertEquals("admin@logistics.com", jwtService.extractUsername(refreshToken));
    }
}
