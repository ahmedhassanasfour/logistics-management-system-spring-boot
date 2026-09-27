package com.ahmed.logistics.auth.controller;

import com.ahmed.logistics.auth.dto.AuthResponse;
import com.ahmed.logistics.auth.dto.LoginRequest;
import com.ahmed.logistics.auth.dto.RefreshTokenRequest;
import com.ahmed.logistics.auth.dto.RegisterRequest;
import com.ahmed.logistics.auth.service.AuthService;
import com.ahmed.logistics.exception.BadRequestException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
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
