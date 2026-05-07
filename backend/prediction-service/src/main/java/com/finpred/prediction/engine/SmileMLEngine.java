package com.finpred.prediction.engine;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Motor de Machine Learning para séries temporais financeiras.
 * 
 * Implementa 3 algoritmos de previsão que competem entre si:
 * 
 * 1. ARMA(p,q) via Smile ML — captura autocorrelação e média móvel
 * 2. Holt-Winters (Triple Exponential Smoothing) — nível + tendência + sazonalidade
 * 3. Regressão Linear + Sazonalidade — fallback robusto para dados simples
 * 
 * Cada algoritmo é treinado no histórico do usuário e avaliado pelo MAPE.
 * O ModelSelectorService decide qual usar para a previsão final.
 */
@Component
@Slf4j
public class SmileMLEngine {

    private static final int SEASONAL_PERIOD = 12; // Dados mensais

    // =========================================================================
    // ALGORITMO 1: ARMA via Smile ML
    // =========================================================================

    /**
     * Previsão usando ARMA(p,q) — Autoregressive Moving Average.
     * 
     * O ARMA combina dois componentes:
     * - AR(p): Valor atual depende dos 'p' valores anteriores
     * - MA(q): Valor atual depende dos 'q' erros anteriores
     * 
     * Requer série estacionária. Se a série original não for estacionária,
     * aplica diferenciação automática (tornando-se efetivamente ARIMA(p,1,q)).
     *
     * @param history  Série temporal histórica (receitas mensais)
     * @param horizon  Número de períodos a prever (ex: 12 meses)
     * @return ForecastResult com previsões e metadados
     */
    public ForecastResult forecastARMA(double[] history, int horizon) {
        try {
            int p = 2; // Ordem autoregressiva
            int q = 1; // Ordem média móvel

            // Verificar estacionariedade e diferenciar se necessário
            double[] workData = history;
            int diffOrder = 0;
            boolean wasDifferenced = false;

            if (!TimeSeriesUtils.isStationary(history)) {
                workData = TimeSeriesUtils.difference(history, 1);
                diffOrder = 1;
                wasDifferenced = true;
                log.debug("Série não-estacionária — aplicada diferenciação de ordem 1");
            }

            // Garantir dados suficientes para ARMA
            if (workData.length < (p + q + 2)) {
                log.warn("Dados insuficientes para ARMA({},{}) — necessário {} pontos, disponível {}",
                        p, q, p + q + 2, workData.length);
                return fallbackLinearForecast(history, horizon, "ARMA_FALLBACK");
            }

            // Ajustar ARMA via Smile
            smile.timeseries.ARMA armaModel = smile.timeseries.ARMA.fit(workData, p, q);

            // Gerar previsões
            double[] forecasts = new double[horizon];
            double[] extendedData = new double[workData.length + horizon];
            System.arraycopy(workData, 0, extendedData, 0, workData.length);

            for (int i = 0; i < horizon; i++) {
                // Previsão um passo à frente usando o modelo ARMA
                double prediction = armaModel.forecast();
                forecasts[i] = prediction;

                // Para previsões multi-step, precisamos adicionar a previsão
                // ao conjunto de dados e re-treinar (rolling forecast)
                extendedData[workData.length + i] = prediction;
            }

            // Se a série foi diferenciada, reverter a diferenciação
            if (wasDifferenced) {
                forecasts = undifference(forecasts, history[history.length - 1]);
            }

            // Aplicar sazonalidade se disponível
            double[] seasonalIndices = TimeSeriesUtils.calculateSeasonalIndices(history, SEASONAL_PERIOD);
            int startMonth = history.length % SEASONAL_PERIOD;
            for (int i = 0; i < horizon; i++) {
                int seasonIndex = (startMonth + i) % SEASONAL_PERIOD;
                forecasts[i] *= seasonalIndices[seasonIndex];
            }

            // Garantir valores positivos para receita
            for (int i = 0; i < forecasts.length; i++) {
                forecasts[i] = Math.max(0, forecasts[i]);
            }

            Map<String, Double> params = new HashMap<>();
            params.put("p", (double) p);
            params.put("q", (double) q);
            params.put("diff_order", (double) diffOrder);

            // Calcular MAPE via holdout dos últimos 3 meses
            double mape = evaluateWithHoldout(history, horizon, "ARMA");

            return ForecastResult.builder()
                    .algorithmUsed(String.format("ARMA(%d,%d)", p, q))
                    .forecastValues(forecasts)
                    .mape(mape)
                    .confidenceScore(Math.max(0, Math.min(100, 100 - mape)))
                    .correctionFactor(1.0)
                    .parameters(params)
                    .build();

        } catch (Exception e) {
            log.error("Erro ao executar ARMA: {} — usando fallback", e.getMessage());
            return fallbackLinearForecast(history, horizon, "ARMA_FALLBACK");
        }
    }

    // =========================================================================
    // ALGORITMO 2: Holt-Winters (Triple Exponential Smoothing)
    // =========================================================================

    /**
     * Previsão usando Holt-Winters multiplicativo.
     * 
     * O Holt-Winters decompõe a série em 3 componentes:
     * - Nível (L): Média suavizada — controlado por α (alpha)
     * - Tendência (T): Direção de crescimento — controlado por β (beta)
     * - Sazonalidade (S): Padrão cíclico — controlado por γ (gamma)
     * 
     * Previsão: F(t+h) = (L_t + h * T_t) * S(t+h-period)
     * 
     * Os parâmetros α, β, γ são otimizados via grid search para minimizar RMSE.
     *
     * @param history  Série temporal histórica
     * @param horizon  Períodos a prever
     * @return ForecastResult com previsões Holt-Winters
     */
    public ForecastResult forecastHoltWinters(double[] history, int horizon) {
        try {
            int period = SEASONAL_PERIOD;

            if (history.length < period * 2) {
                log.warn("Holt-Winters requer ao menos 2 ciclos completos ({} pontos). Disponível: {}",
                        period * 2, history.length);
                return fallbackLinearForecast(history, horizon, "HW_FALLBACK");
            }

            // Grid search para encontrar melhores parâmetros
            double bestAlpha = 0.3, bestBeta = 0.1, bestGamma = 0.2;
            double bestRMSE = Double.MAX_VALUE;

            for (double alpha = 0.1; alpha <= 0.9; alpha += 0.2) {
                for (double beta = 0.01; beta <= 0.5; beta += 0.1) {
                    for (double gamma = 0.05; gamma <= 0.5; gamma += 0.1) {
                        double rmse = computeHoltWintersRMSE(history, period, alpha, beta, gamma);
                        if (rmse < bestRMSE) {
                            bestRMSE = rmse;
                            bestAlpha = alpha;
                            bestBeta = beta;
                            bestGamma = gamma;
                        }
                    }
                }
            }

            log.debug("Holt-Winters otimizado: α={}, β={}, γ={}, RMSE={}",
                    bestAlpha, bestBeta, bestGamma, bestRMSE);

            // Executar Holt-Winters com os melhores parâmetros
            double[] forecasts = runHoltWinters(history, period, bestAlpha, bestBeta, bestGamma, horizon);

            // Garantir valores positivos
            for (int i = 0; i < forecasts.length; i++) {
                forecasts[i] = Math.max(0, forecasts[i]);
            }

            Map<String, Double> params = new HashMap<>();
            params.put("alpha", bestAlpha);
            params.put("beta", bestBeta);
            params.put("gamma", bestGamma);
            params.put("period", (double) period);
            params.put("rmse", bestRMSE);

            double mape = evaluateWithHoldout(history, horizon, "HOLT_WINTERS");

            return ForecastResult.builder()
                    .algorithmUsed(String.format("HOLT_WINTERS(α=%.2f,β=%.2f,γ=%.2f)", bestAlpha, bestBeta, bestGamma))
                    .forecastValues(forecasts)
                    .mape(mape)
                    .confidenceScore(Math.max(0, Math.min(100, 100 - mape)))
                    .correctionFactor(1.0)
                    .parameters(params)
                    .build();

        } catch (Exception e) {
            log.error("Erro ao executar Holt-Winters: {} — usando fallback", e.getMessage());
            return fallbackLinearForecast(history, horizon, "HW_FALLBACK");
        }
    }

    /**
     * Executa o algoritmo Holt-Winters multiplicativo completo.
     */
    private double[] runHoltWinters(double[] data, int period, double alpha, double beta, double gamma, int horizon) {
        int n = data.length;

        // Inicialização do nível: média do primeiro ciclo
        double level = 0;
        for (int i = 0; i < period; i++) {
            level += data[i];
        }
        level /= period;

        // Inicialização da tendência: diferença média entre dois ciclos
        double trend = 0;
        if (n >= period * 2) {
            for (int i = 0; i < period; i++) {
                trend += (data[period + i] - data[i]);
            }
            trend /= (period * period);
        }

        // Inicialização da sazonalidade: razão entre valor e nível
        double[] seasonal = new double[n + horizon];
        for (int i = 0; i < period; i++) {
            seasonal[i] = level > 1e-10 ? data[i] / level : 1.0;
        }

        // Suavização exponencial
        double[] levels = new double[n];
        double[] trends = new double[n];
        levels[0] = level;
        trends[0] = trend;

        for (int t = 1; t < n; t++) {
            // Proteção contra sazonalidade zero
            double prevSeasonal = seasonal[t - 1 >= 0 ? t : 0];
            if (Math.abs(prevSeasonal) < 1e-10) prevSeasonal = 1.0;

            // Atualizar nível
            int seasonIdx = (t - period) >= 0 ? (t - period) : (t % period);
            double seasonFactor = seasonal[seasonIdx];
            if (Math.abs(seasonFactor) < 1e-10) seasonFactor = 1.0;

            double newLevel = alpha * (data[t] / seasonFactor) + (1 - alpha) * (levels[t - 1] + trends[t - 1]);

            // Atualizar tendência
            double newTrend = beta * (newLevel - levels[t - 1]) + (1 - beta) * trends[t - 1];

            // Atualizar sazonalidade
            double newSeasonal = gamma * (data[t] / newLevel) + (1 - gamma) * seasonFactor;

            levels[t] = newLevel;
            trends[t] = newTrend;
            seasonal[t] = newSeasonal;
        }

        // Previsão
        double[] forecasts = new double[horizon];
        double lastLevel = levels[n - 1];
        double lastTrend = trends[n - 1];

        for (int h = 1; h <= horizon; h++) {
            int seasonIdx = n - period + ((h - 1) % period);
            if (seasonIdx < 0) seasonIdx = (h - 1) % period;
            double seasonFactor = seasonal[seasonIdx];
            if (Math.abs(seasonFactor) < 1e-10) seasonFactor = 1.0;

            forecasts[h - 1] = (lastLevel + h * lastTrend) * seasonFactor;
        }

        return forecasts;
    }

    /**
     * Calcula o RMSE interno de Holt-Winters para otimização de parâmetros.
     */
    private double computeHoltWintersRMSE(double[] data, int period, double alpha, double beta, double gamma) {
        try {
            int n = data.length;
            int holdout = Math.min(3, n / 4);
            int trainSize = n - holdout;

            if (trainSize < period) return Double.MAX_VALUE;

            double[] train = new double[trainSize];
            System.arraycopy(data, 0, train, 0, trainSize);

            double[] predicted = runHoltWinters(train, period, alpha, beta, gamma, holdout);
            double[] actual = new double[holdout];
            System.arraycopy(data, trainSize, actual, 0, holdout);

            return TimeSeriesUtils.calculateRMSE(actual, predicted);
        } catch (Exception e) {
            return Double.MAX_VALUE;
        }
    }

    // =========================================================================
    // ALGORITMO 3: Regressão Linear + Sazonalidade
    // =========================================================================

    /**
     * Previsão usando Regressão Linear com ajuste sazonal.
     * 
     * Este é o modelo base (fallback robusto):
     * 1. Ajusta regressão linear (tendência)
     * 2. Calcula índices sazonais dos resíduos
     * 3. Combina: Previsão = Tendência(t) × Sazonalidade(t)
     *
     * Funciona bem para dados com tendência clara e sazonalidade regular.
     *
     * @param history  Série temporal histórica
     * @param horizon  Períodos a prever
     * @return ForecastResult com previsões
     */
    public ForecastResult forecastLinearRegression(double[] history, int horizon) {
        SimpleRegression regression = new SimpleRegression();
        for (int i = 0; i < history.length; i++) {
            regression.addData(i, history[i]);
        }

        double slope = regression.getSlope();
        double intercept = regression.getIntercept();
        double rSquared = regression.getRSquare();

        // Calcular índices sazonais
        double[] seasonalIndices = TimeSeriesUtils.calculateSeasonalIndices(history, SEASONAL_PERIOD);

        // Gerar previsões
        double[] forecasts = new double[horizon];
        int startIndex = history.length;
        int startMonth = history.length % SEASONAL_PERIOD;

        for (int i = 0; i < horizon; i++) {
            double trendValue = intercept + slope * (startIndex + i);
            int seasonIndex = (startMonth + i) % SEASONAL_PERIOD;
            forecasts[i] = Math.max(0, trendValue * seasonalIndices[seasonIndex]);
        }

        Map<String, Double> params = new HashMap<>();
        params.put("slope", slope);
        params.put("intercept", intercept);
        params.put("r_squared", Double.isNaN(rSquared) ? 0.0 : rSquared);

        double mape = evaluateWithHoldout(history, horizon, "LINEAR_REGRESSION");

        return ForecastResult.builder()
                .algorithmUsed("LINEAR_REGRESSION")
                .forecastValues(forecasts)
                .mape(mape)
                .confidenceScore(Math.max(0, Math.min(100, 100 - mape)))
                .correctionFactor(1.0)
                .parameters(params)
                .build();
    }

    // =========================================================================
    // UTILITÁRIOS INTERNOS
    // =========================================================================

    /**
     * Avalia a qualidade de um modelo usando holdout cross-validation.
     *
     * Remove os últimos 3 meses do histórico, treina o modelo nos dados restantes,
     * prevê os 3 meses e calcula o MAPE contra os valores reais.
     */
    private double evaluateWithHoldout(double[] history, int horizon, String algorithm) {
        int holdoutSize = Math.min(3, history.length / 3);
        if (holdoutSize < 1 || history.length - holdoutSize < 6) {
            return 50.0; // Dados insuficientes — MAPE neutro
        }

        int trainSize = history.length - holdoutSize;
        double[] trainData = new double[trainSize];
        double[] holdoutActual = new double[holdoutSize];

        System.arraycopy(history, 0, trainData, 0, trainSize);
        System.arraycopy(history, trainSize, holdoutActual, 0, holdoutSize);

        try {
            double[] holdoutPredicted;
            switch (algorithm) {
                case "ARMA" -> {
                    double[] workData = trainData;
                    if (!TimeSeriesUtils.isStationary(trainData)) {
                        workData = TimeSeriesUtils.difference(trainData, 1);
                    }
                    if (workData.length >= 5) {
                        smile.timeseries.ARMA model = smile.timeseries.ARMA.fit(workData, 2, 1);
                        holdoutPredicted = new double[holdoutSize];
                        for (int i = 0; i < holdoutSize; i++) {
                            holdoutPredicted[i] = model.forecast();
                        }
                        if (!TimeSeriesUtils.isStationary(trainData)) {
                            holdoutPredicted = undifference(holdoutPredicted, trainData[trainData.length - 1]);
                        }
                    } else {
                        return 50.0;
                    }
                }
                case "HOLT_WINTERS" -> {
                    if (trainData.length >= SEASONAL_PERIOD * 2) {
                        holdoutPredicted = runHoltWinters(trainData, SEASONAL_PERIOD, 0.3, 0.1, 0.2, holdoutSize);
                    } else {
                        return 50.0;
                    }
                }
                default -> {
                    // LINEAR_REGRESSION
                    SimpleRegression reg = new SimpleRegression();
                    for (int i = 0; i < trainData.length; i++) {
                        reg.addData(i, trainData[i]);
                    }
                    double[] indices = TimeSeriesUtils.calculateSeasonalIndices(trainData, SEASONAL_PERIOD);
                    holdoutPredicted = new double[holdoutSize];
                    int startMonth = trainData.length % SEASONAL_PERIOD;
                    for (int i = 0; i < holdoutSize; i++) {
                        double trend = reg.getIntercept() + reg.getSlope() * (trainData.length + i);
                        int sIdx = (startMonth + i) % SEASONAL_PERIOD;
                        holdoutPredicted[i] = Math.max(0, trend * indices[sIdx]);
                    }
                }
            }

            return TimeSeriesUtils.calculateMAPE(holdoutActual, holdoutPredicted);

        } catch (Exception e) {
            log.debug("Erro na avaliação holdout de {}: {}", algorithm, e.getMessage());
            return 50.0;
        }
    }

    /**
     * Reverte a diferenciação de uma série.
     * Se diff[i] = y[i] - y[i-1], então y[i] = lastValue + Σdiff[0..i]
     */
    private double[] undifference(double[] diffed, double lastOriginalValue) {
        double[] result = new double[diffed.length];
        double cumulative = lastOriginalValue;

        for (int i = 0; i < diffed.length; i++) {
            cumulative += diffed[i];
            result[i] = cumulative;
        }

        return result;
    }

    /**
     * Previsão fallback usando regressão linear simples quando
     * o algoritmo principal falha.
     */
    private ForecastResult fallbackLinearForecast(double[] history, int horizon, String reason) {
        log.info("Usando fallback de regressão linear (razão: {})", reason);
        return forecastLinearRegression(history, horizon);
    }
}
