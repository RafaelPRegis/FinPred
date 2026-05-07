package com.finpred.prediction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Resposta de um feedback registrado, com cálculos de erro.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResponse {

    private Long id;
    private Long predictionId;
    private LocalDate month;

    /** Valor que foi previsto */
    private BigDecimal predictedValue;

    /** Valor real informado pelo usuário */
    private BigDecimal actualValue;

    /** Erro percentual absoluto: |actual - predicted| / |actual| * 100 */
    private BigDecimal errorPercentage;

    /** MAPE acumulado do usuário até este feedback */
    private BigDecimal mapeScore;

    /** Fator de correção calculado */
    private BigDecimal correctionFactor;

    /** Data de criação do feedback */
    private LocalDateTime createdAt;
}
