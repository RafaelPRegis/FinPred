package com.finpred.prediction.engine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO interno que encapsula o resultado de uma previsão do motor de ML.
 * 
 * Contém os valores projetados, o algoritmo utilizado, a métrica de qualidade
 * (MAPE) e os parâmetros do modelo para auditoria e transparência.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForecastResult {

    /**
     * Nome do algoritmo usado para gerar a previsão.
     * Exemplos: "ARMA(2,1)", "HOLT_WINTERS(α=0.3,β=0.1,γ=0.2)", "LINEAR_REGRESSION"
     */
    private String algorithmUsed;

    /**
     * Valores projetados para os próximos N períodos.
     * Para projeção anual, contém 12 valores (um por mês).
     */
    private double[] forecastValues;

    /**
     * MAPE (Mean Absolute Percentage Error) do modelo selecionado,
     * calculado via cross-validation no histórico do usuário.
     * Quanto menor, melhor (< 10% = altamente preciso).
     */
    private double mape;

    /**
     * Score de confiança derivado do MAPE.
     * Calculado como: max(0, min(100, 100 - MAPE))
     * 
     * Um MAPE de 5% resulta em confidência de 95%.
     */
    private double confidenceScore;

    /**
     * Fator de correção automático baseado no viés histórico.
     * Já aplicado aos forecastValues.
     * 
     * > 1.0 = modelo tendia a subestimar
     * < 1.0 = modelo tendia a superestimar
     * = 1.0 = sem viés detectado
     */
    private double correctionFactor;

    /**
     * Parâmetros internos do modelo para auditoria.
     * Varia conforme o algoritmo:
     * - ARMA: {p, q, ar_coefficients, ma_coefficients}
     * - Holt-Winters: {alpha, beta, gamma, period}
     * - Regressão: {slope, intercept, r_squared}
     */
    private Map<String, Double> parameters;

    /**
     * Classificação textual da qualidade baseada no MAPE.
     */
    public String getQualityClassification() {
        return TimeSeriesUtils.classifyMAPEDisplay(mape);
    }

    /**
     * Retorna true se o modelo é considerado confiável (MAPE < 20%).
     */
    public boolean isReliable() {
        return mape < 20.0;
    }
}
