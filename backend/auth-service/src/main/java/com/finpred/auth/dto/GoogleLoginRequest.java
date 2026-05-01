package com.finpred.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para login com Google OAuth2.
 * O frontend envia o credential (ID token) obtido do Google Identity Services.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleLoginRequest {

    @NotBlank(message = "Token do Google é obrigatório")
    private String credential;
}
