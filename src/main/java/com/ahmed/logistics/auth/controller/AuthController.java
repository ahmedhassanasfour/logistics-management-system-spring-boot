package com.ahmed.logistics.auth.controller;

import com.ahmed.logistics.auth.dto.AuthResponse;
import com.ahmed.logistics.auth.dto.LoginRequest;
import com.ahmed.logistics.auth.dto.RefreshTokenRequest;
import com.ahmed.logistics.auth.dto.RegisterRequest;
import com.ahmed.logistics.auth.service.AuthService;
import com.ahmed.logistics.exception.BadRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication", description = "User registration, authentication, and JWT token refresh APIs")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new user", description = "Creates a new user account with specified role and returns JWT tokens.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User successfully registered"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or validation error"),
            @ApiResponse(responseCode = "409", description = "Email already in use")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Authenticate user", description = "Validates user credentials and issues JWT access and refresh tokens.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful"),
            @ApiResponse(responseCode = "400", description = "Invalid login payload"),
            @ApiResponse(responseCode = "401", description = "Invalid email or password")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Refresh JWT access token", description = "Generates a fresh JWT access token using a valid refresh token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "400", description = "Refresh token is missing or malformed"),
            @ApiResponse(responseCode = "401", description = "Invalid, expired, or non-refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Parameter(description = "Bearer refresh token in Authorization header")
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) RefreshTokenRequest request
    ) {
        String token = null;

        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            token = request.refreshToken().trim();
        } else if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7).trim();
        }

        if (token == null || token.isBlank()) {
            throw new BadRequestException("Refresh token is required");
        }

        AuthResponse response = authService.refreshToken(token);
        return ResponseEntity.ok(response);
    }
}
