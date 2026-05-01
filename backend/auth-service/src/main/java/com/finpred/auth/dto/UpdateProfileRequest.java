package com.finpred.auth.dto;

import com.finpred.auth.model.BusinessType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @NotBlank(message = "O nome é obrigatório")
    private String username;

    @NotBlank(message = "O nome da empresa é obrigatório")
    private String companyName;

    @NotNull(message = "O setor de atuação é obrigatório")
    private BusinessType businessType;

    @NotBlank(message = "A natureza jurídica é obrigatória")
    private String legalNature;

    // A senha é opcional. Se enviada, será atualizada.
    private String password;
}
