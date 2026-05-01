package com.finpred.prediction.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationRequest {
    @NotNull(message = "O preço base é obrigatório")
    private BigDecimal basePrice;

    @NotNull(message = "O custo base é obrigatório")
    private BigDecimal baseCost;

    @NotNull(message = "O volume de vendas é obrigatório")
    private Integer baseVolume;

    // Crescimento esperado em % (ex: 5.0 para 5%)
    @NotNull(message = "A taxa de crescimento esperada é obrigatória")
    private BigDecimal expectedGrowthRate;
}
