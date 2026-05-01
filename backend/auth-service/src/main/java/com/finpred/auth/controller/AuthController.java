package com.finpred.auth.controller;

import com.finpred.auth.dto.*;
import com.finpred.auth.service.AuthService;
import com.finpred.auth.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Controller de autenticação.
 * Endpoints: register (2 etapas), login, google, profile, health.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    /**
     * POST /api/auth/register — Etapa 1 do cadastro.
     * Body: { email, username, companyName, password }
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerStepOne(@Valid @RequestBody RegisterStepOneRequest request) {
        try {
            AuthResponse response = authService.registerStepOne(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/auth/register/complete — Etapa 2 do cadastro.
     * Requer JWT do step 1. Body: { businessType, legalNature }
     */
    @PutMapping("/register/complete")
    public ResponseEntity<?> registerStepTwo(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody RegisterStepTwoRequest request) {
        try {
            Long userId = extractUserId(authHeader);
            UserProfileResponse response = authService.registerStepTwo(userId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/auth/login — Login com email e senha.
     * Body: { email, password }
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/auth/google — Login com Google OAuth2.
     * Body: { credential }
     */
    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        try {
            AuthResponse response = authService.googleLogin(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/auth/profile — Retorna o perfil do usuário autenticado.
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = extractUserId(authHeader);
            UserProfileResponse response = authService.getProfile(userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Token inválido"));
        }
    }

    /**
     * PUT /api/auth/profile — Atualiza o perfil do usuário.
     */
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody UpdateProfileRequest request) {
        try {
            Long userId = extractUserId(authHeader);
            UserProfileResponse response = authService.updateProfile(userId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Token inválido"));
        }
    }

    /**
     * GET /api/auth/health — Health check.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "service", "auth-service",
                "status", "UP",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * Extrai o userId do header Authorization (Bearer token).
     */
    private Long extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Token não fornecido");
        }
        String token = authHeader.substring(7);
        return jwtUtil.extractUserId(token);
    }
}
