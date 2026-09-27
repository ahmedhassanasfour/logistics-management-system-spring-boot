package com.ahmed.logistics.auth.service;

import com.ahmed.logistics.auth.dto.AuthResponse;
import com.ahmed.logistics.auth.dto.LoginRequest;
import com.ahmed.logistics.auth.dto.RegisterRequest;
import com.ahmed.logistics.auth.jwt.JwtService;
import com.ahmed.logistics.auth.security.CustomUserDetails;
import com.ahmed.logistics.exception.BadRequestException;
import com.ahmed.logistics.exception.UnauthorizedException;
import com.ahmed.logistics.user.entity.Role;
import com.ahmed.logistics.user.entity.User;
import com.ahmed.logistics.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;
    private CustomUserDetails sampleUserDetails;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .email("user@logistics.com")
                .password("$2a$10$encodedPassword")
                .firstName("Test")
                .lastName("User")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();

        sampleUserDetails = new CustomUserDetails(sampleUser);
    }

    @Test
    void register_createsCustomerAndHashesPassword() {
        RegisterRequest request = new RegisterRequest(
                "user@logistics.com",
                "plainPassword123",
                "Test",
                "User",
                Role.ADMIN // Should be overridden to CUSTOMER
        );

        when(userRepository.existsByEmail("user@logistics.com")).thenReturn(false);
        when(passwordEncoder.encode("plainPassword123")).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtService.generateAccessToken(any(CustomUserDetails.class))).thenReturn("access_token_123");
        when(jwtService.generateRefreshToken(any(CustomUserDetails.class))).thenReturn("refresh_token_123");
        when(jwtService.getAccessTokenExpiration()).thenReturn(900000L);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("access_token_123", response.accessToken());
        assertEquals("refresh_token_123", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900000L, response.expiresIn());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();

        assertEquals("user@logistics.com", capturedUser.getEmail());
        assertEquals("$2a$10$encodedPassword", capturedUser.getPassword());
        assertEquals(Role.CUSTOMER, capturedUser.getRole(), "Public registration must always assign CUSTOMER role");
    }

    @Test
    void register_duplicateEmailFails() {
        RegisterRequest request = new RegisterRequest(
                "existing@logistics.com",
                "plainPassword123",
                "Test",
                "User",
                null
        );

        when(userRepository.existsByEmail("existing@logistics.com")).thenReturn(true);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> authService.register(request)
        );

        assertTrue(ex.getMessage().contains("already exists"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_succeedsWithCorrectCredentials() {
        LoginRequest request = new LoginRequest("user@logistics.com", "plainPassword123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(userDetailsService.loadUserByUsername("user@logistics.com")).thenReturn(sampleUserDetails);
        when(jwtService.generateAccessToken(sampleUserDetails)).thenReturn("access_token_abc");
        when(jwtService.generateRefreshToken(sampleUserDetails)).thenReturn("refresh_token_abc");
        when(jwtService.getAccessTokenExpiration()).thenReturn(900000L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access_token_abc", response.accessToken());
        assertEquals("refresh_token_abc", response.refreshToken());
    }

    @Test
    void login_failsWithIncorrectCredentials() {
        LoginRequest request = new LoginRequest("user@logistics.com", "wrongPassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> authService.login(request)
        );

        assertEquals("Invalid email or password", ex.getMessage());
    }

    @Test
    void refreshToken_generatesNewAccessToken() {
        String refreshToken = "valid_refresh_token";

        when(jwtService.isRefreshToken(refreshToken)).thenReturn(true);
        when(jwtService.extractUsername(refreshToken)).thenReturn("user@logistics.com");
        when(userDetailsService.loadUserByUsername("user@logistics.com")).thenReturn(sampleUserDetails);
        when(jwtService.isTokenValid(refreshToken, sampleUserDetails)).thenReturn(true);
        when(jwtService.generateAccessToken(sampleUserDetails)).thenReturn("new_access_token_xyz");
        when(jwtService.getAccessTokenExpiration()).thenReturn(900000L);

        AuthResponse response = authService.refreshToken(refreshToken);

        assertNotNull(response);
        assertEquals("new_access_token_xyz", response.accessToken());
        assertEquals(refreshToken, response.refreshToken());
    }

    @Test
    void refreshToken_failsWithInvalidOrAccessToken() {
        String invalidToken = "not_a_refresh_token";
        when(jwtService.isRefreshToken(invalidToken)).thenReturn(false);

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> authService.refreshToken(invalidToken)
        );

        assertTrue(ex.getMessage().contains("Invalid refresh token"));
    }
}
