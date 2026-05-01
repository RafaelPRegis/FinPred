package com.finpred.auth.dto;

import com.finpred.auth.model.BusinessType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para o segundo passo do registro.
 * Coleta: tipo de negócio e natureza jurídica.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterStepTwoRequest {

    @NotNull(message = "Tipo de negócio é obrigatório")
    private BusinessType businessType;

    @NotBlank(message = "Natureza jurídica é obrigatória")
    private String legalNature;
}
