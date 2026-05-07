package com.finpred.prediction.engine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Seletor automático de algoritmo de previsão.
 * 
 * Executa todos os algoritmos disponíveis no SmileMLEngine sobre o mesmo
 * histórico, avalia o MAPE de cada um via cross-validation (holdout dos
 * últimos 3 meses), e retorna o modelo com menor erro.
 * 
 * Isso garante que o sistema sempre usa o melhor algoritmo para cada
 * perfil de dados — dados lineares serão melhor previstos por regressão,
 * enquanto dados com sazonalidade forte serão melhor previstos por
 * Holt-Winters ou ARMA.
 * 
 * Threshold mínimo de ativação ML: 6 meses de histórico.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ModelSelectorService {

    private final SmileMLEngine smileMLEngine;

    /** Número mínimo de pontos de dados para ativar ML avançado */
    private static final int MIN_DATA_POINTS_FOR_ML = 6;

    /** Número mínimo de pontos para Holt-Winters (precisa de 2 ciclos) */
    private static final int MIN_DATA_POINTS_FOR_HW = 24;

    /**
     * Seleciona e executa o melhor algoritmo de previsão para os dados fornecidos.
     * 
     * Workflow:
     * 1. Se dados < 6 meses → usa apenas regressão linear (fallback seguro)
     * 2. Se dados >= 6 meses → compete ARMA vs Regressão
     * 3. Se dados >= 24 meses → compete ARMA vs Holt-Winters vs Regressão
     * 4. Retorna o resultado com menor MAPE
     * 
     * @param monthlyRevenue Receita mensal histórica (ordenada cronologicamente)
     * @param horizon        Número de meses a prever (ex: 12)
     * @return ForecastResult do melhor modelo
     */
    public ForecastResult selectBestModel(double[] monthlyRevenue, int horizon) {
        log.info("Selecionando melhor modelo para {} pontos de dados, horizonte={}", 
                monthlyRevenue.length, horizon);

        // Dados insuficientes — fallback direto
        if (monthlyRevenue.length < MIN_DATA_POINTS_FOR_ML) {
            log.info("Dados insuficientes para ML ({} < {}). Usando regressão linear.",
                    monthlyRevenue.length, MIN_DATA_POINTS_FOR_ML);
            return smileMLEngine.forecastLinearRegression(monthlyRevenue, horizon);
        }

        List<ForecastResult> candidates = new ArrayList<>();

        // Sempre compete: Regressão Linear + Sazonalidade
        try {
            ForecastResult linearResult = smileMLEngine.forecastLinearRegression(monthlyRevenue, horizon);
            candidates.add(linearResult);
            log.debug("LINEAR_REGRESSION → MAPE: {:.2f}%", linearResult.getMape());
        } catch (Exception e) {
            log.error("Erro na regressão linear: {}", e.getMessage());
        }

        // Com 6+ meses: adiciona ARMA
        if (monthlyRevenue.length >= MIN_DATA_POINTS_FOR_ML) {
            try {
                ForecastResult armaResult = smileMLEngine.forecastARMA(monthlyRevenue, horizon);
                candidates.add(armaResult);
                log.debug("ARMA → MAPE: {:.2f}%", armaResult.getMape());
            } catch (Exception e) {
                log.warn("ARMA falhou: {}", e.getMessage());
            }
        }

        // Com 24+ meses: adiciona Holt-Winters
        if (monthlyRevenue.length >= MIN_DATA_POINTS_FOR_HW) {
            try {
                ForecastResult hwResult = smileMLEngine.forecastHoltWinters(monthlyRevenue, horizon);
                candidates.add(hwResult);
                log.debug("HOLT_WINTERS → MAPE: {:.2f}%", hwResult.getMape());
            } catch (Exception e) {
                log.warn("Holt-Winters falhou: {}", e.getMessage());
            }
        }

        // Se nenhum candidato sobreviveu, fallback de emergência
        if (candidates.isEmpty()) {
            log.error("Todos os algoritmos falharam! Usando regressão linear de emergência.");
            return smileMLEngine.forecastLinearRegression(monthlyRevenue, horizon);
        }

        // Selecionar o modelo com menor MAPE
        ForecastResult best = candidates.stream()
                .min(Comparator.comparingDouble(ForecastResult::getMape))
                .orElse(candidates.get(0));

        log.info("✅ Modelo selecionado: {} (MAPE: {:.2f}%, Confiança: {:.1f}%)",
                best.getAlgorithmUsed(), best.getMape(), best.getConfidenceScore());

        // Log de comparação para auditoria
        candidates.forEach(c -> 
            log.info("  → {}: MAPE={:.2f}% {}", 
                c.getAlgorithmUsed(), 
                c.getMape(),
                c == best ? "✅ SELECIONADO" : "")
        );

        return best;
    }

    /**
     * Retorna informações sobre todos os modelos avaliados sem selecionar.
     * Útil para debug e para mostrar ao usuário a comparação entre algoritmos.
     *
     * @param monthlyRevenue Receita mensal histórica
     * @param horizon        Horizonte de previsão
     * @return Lista de ForecastResult de todos os modelos avaliados
     */
    public List<ForecastResult> evaluateAllModels(double[] monthlyRevenue, int horizon) {
        List<ForecastResult> results = new ArrayList<>();

        try {
            results.add(smileMLEngine.forecastLinearRegression(monthlyRevenue, horizon));
        } catch (Exception e) {
            log.debug("Linear regression evaluation failed: {}", e.getMessage());
        }

        if (monthlyRevenue.length >= MIN_DATA_POINTS_FOR_ML) {
            try {
                results.add(smileMLEngine.forecastARMA(monthlyRevenue, horizon));
            } catch (Exception e) {
                log.debug("ARMA evaluation failed: {}", e.getMessage());
            }
        }

        if (monthlyRevenue.length >= MIN_DATA_POINTS_FOR_HW) {
            try {
                results.add(smileMLEngine.forecastHoltWinters(monthlyRevenue, horizon));
            } catch (Exception e) {
                log.debug("Holt-Winters evaluation failed: {}", e.getMessage());
            }
        }

        return results;
    }
}
