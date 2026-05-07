package com.finpred.prediction.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request para submeter feedback (valor real vs previsão).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackRequest {

    /** ID da predição que está sendo avaliada */
    @NotNull(message = "O ID da predição é obrigatório")
    private Long predictionId;

    /** Mês/ano do feedback (formato ISO: 2026-01-01) */
    @NotNull(message = "O mês de referência é obrigatório")
    private LocalDate month;

    /** Valor real observado no período */
    @NotNull(message = "O valor realizado é obrigatório")
    private BigDecimal actualValue;
}
