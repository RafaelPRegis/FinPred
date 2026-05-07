package com.finpred.prediction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Item do histórico de predições de um usuário.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionHistoryItem {

    private Long id;

    /** Mês da predição */
    private LocalDate month;

    /** Label formatado (ex: "Jan/2026") */
    private String monthLabel;

    /** Receita prevista (cenário neutro) */
    private BigDecimal predictedRevenue;

    /** Score de confiança (0-100) */
    private BigDecimal confidenceScore;

    /** Nome do algoritmo usado */
    private String algorithm;

    /** Data de criação da predição */
    private LocalDateTime createdAt;
}
