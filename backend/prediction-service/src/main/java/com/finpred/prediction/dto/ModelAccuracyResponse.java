package com.finpred.prediction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Resposta com a acurácia do modelo de predição do usuário.
 * Inclui MAPE, classificação textual, total de feedbacks e melhor algoritmo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelAccuracyResponse {

    /** MAPE (Mean Absolute Percentage Error) em porcentagem */
    private BigDecimal mape;

    /** Classificação textual: "Altamente Preciso", "Bom", "Razoável", "Impreciso" */
    private String classification;

    /** Emoji/ícone correspondente à classificação */
    private String classificationIcon;

    /** Total de feedbacks registrados pelo usuário */
    private long totalFeedbacks;

    /** Fator de correção automático atual */
    private BigDecimal correctionFactor;

    /** Nome do melhor algoritmo em uso */
    private String bestAlgorithm;

    /** Score de confiança (100 - MAPE), clamped 0-100 */
    private BigDecimal confidenceScore;
}
