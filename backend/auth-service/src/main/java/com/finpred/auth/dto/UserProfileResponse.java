package com.finpred.auth.dto;

import com.finpred.auth.model.AuthProvider;
import com.finpred.auth.model.BusinessType;
import com.finpred.auth.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de resposta com perfil completo do usuário.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private Long id;
    private String email;
    private String username;
    private String companyName;
    private AuthProvider provider;
    private BusinessType businessType;
    private String legalNature;
    private boolean profileComplete;
    private LocalDateTime createdAt;

    public static UserProfileResponse fromUser(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .companyName(user.getCompanyName())
                .provider(user.getProvider())
                .businessType(user.getBusinessType())
                .legalNature(user.getLegalNature())
                .profileComplete(user.getBusinessType() != null && user.getLegalNature() != null)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
