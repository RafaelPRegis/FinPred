package com.finpred.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de resposta de autenticação.
 * Retorna o JWT token e dados básicos do usuário.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String type;
    private Long userId;
    private String email;
    private String username;
    private boolean profileComplete;
    private String message;

    public static AuthResponse success(String token, Long userId, String email, String username, boolean profileComplete) {
        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(userId)
                .email(email)
                .username(username)
                .profileComplete(profileComplete)
                .message("Autenticação realizada com sucesso")
                .build();
    }
}
