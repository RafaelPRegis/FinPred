package com.finpred.prediction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Resposta da previsão ML com cenários, metadados do algoritmo
 * e score de confiança.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForecastResponse {

    /** Cenário pessimista (×0.85) */
    private ScenarioResult pessimist;

    /** Cenário neutro (×1.00 — previsão base) */
    private ScenarioResult neutral;

    /** Cenário otimista (×1.15) */
    private ScenarioResult optimist;

    /** Nome do algoritmo usado (ex: "ARMA(2,1)", "HOLT_WINTERS") */
    private String algorithmUsed;

    /** MAPE do modelo selecionado */
    private BigDecimal mape;

    /** Score de confiança (0-100) */
    private BigDecimal confidenceScore;

    /** Classificação textual do MAPE */
    private String qualityClassification;

    /** Fator de correção aplicado */
    private BigDecimal correctionFactor;

    /** Parâmetros internos do modelo para transparência */
    private Map<String, Double> modelParameters;

    /** Quantidade de meses de histórico usados no treinamento */
    private int historyMonths;
}
