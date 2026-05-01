package com.finpred.core.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxSimulationRequest {

    @NotNull(message = "Receita mensal simulada é obrigatória")
    @PositiveOrZero(message = "A receita deve ser positiva")
    private BigDecimal revenue;

    @NotNull(message = "Regime tributário é obrigatório (SIMPLES_NACIONAL, LUCRO_PRESUMIDO)")
    private String taxRegime;

    private String legalNature;

    @NotNull(message = "Área de atuação é obrigatória (ex: COMERCIO, INDUSTRIA, SERVICO)")
    private String businessType;

    // Para o Simples Nacional
    @PositiveOrZero(message = "O RBT12 deve ser positivo")
    private BigDecimal rbt12;

    @PositiveOrZero(message = "A folha de pagamento deve ser positiva")
    private BigDecimal payrollAmount;
}
