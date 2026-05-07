package com.finpred.prediction.service;

import com.finpred.prediction.dto.*;
import com.finpred.prediction.engine.ForecastResult;
import com.finpred.prediction.engine.ModelSelectorService;
import com.finpred.prediction.engine.TimeSeriesUtils;
import com.finpred.prediction.model.PredictionResult;
import com.finpred.prediction.model.Scenario;
import com.finpred.prediction.repository.PredictionResultRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Motor de Predição Financeira — Orquestrador Principal.
 * 
 * Combina duas modalidades de previsão:
 * 
 * 1. SIMULAÇÃO PARAMÉTRICA: O usuário fornece preço, custo, volume e crescimento.
 *    Gera projeção de 12 meses com cenários. Não requer histórico real.
 *    
 * 2. PREVISÃO ML (Fase 5): Usa o histórico real de transações do usuário.
 *    O ModelSelectorService escolhe o melhor algoritmo (ARMA, Holt-Winters, 
 *    ou Regressão Linear) e gera previsão com MAPE e fator de correção.
 *    Requer mínimo de 6 meses de dados.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PredictionEngineService {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMM/yyyy");
    private static final int FORECAST_HORIZON = 12;
    private static final int MIN_MONTHS_FOR_ML = 6;

    private final ModelSelectorService modelSelectorService;
    private final PredictionResultRepository predictionResultRepository;
    private final FeedbackService feedbackService;
    private final ObjectMapper objectMapper;

    // =========================================================================
    // MODALIDADE 1: SIMULAÇÃO PARAMÉTRICA (comportamento existente, preservado)
    // =========================================================================

    /**
     * Simulação paramétrica — entrada direta do usuário (sliders).
     * Este é o fluxo original do SimulatorPage.
     */
    public SimulationResponse simulate(SimulationRequest request) {
        log.info("Processando simulação paramétrica: {}", request);

        ScenarioResult neutral = generateScenario(request, "NEUTRAL", BigDecimal.valueOf(1.0));
        ScenarioResult optimist = generateScenario(request, "OPTIMIST", BigDecimal.valueOf(1.15));
        ScenarioResult pessimist = generateScenario(request, "PESSIMIST", BigDecimal.valueOf(0.85));

        return SimulationResponse.builder()
                .neutral(neutral)
                .optimist(optimist)
                .pessimist(pessimist)
                .build();
    }

    private ScenarioResult generateScenario(SimulationRequest request, String name, BigDecimal scenarioMultiplier) {
        List<MonthlyProjection> projections = new ArrayList<>();
        LocalDate currentDate = LocalDate.now();
        BigDecimal totalProfit = BigDecimal.ZERO;

        BigDecimal currentPrice = request.getBasePrice();
        BigDecimal currentCost = request.getBaseCost();
        BigDecimal currentVolume = BigDecimal.valueOf(request.getBaseVolume());

        // A taxa de crescimento esperada (ex: 5%) é convertida em multiplicador mensal
        // Se a taxa for 5% anual, mensal = 5 / 12 = 0.41%
        BigDecimal annualGrowthRate = request.getExpectedGrowthRate().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal monthlyGrowthMultiplier = BigDecimal.ONE.add(annualGrowthRate.divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP));

        for (int i = 1; i <= 12; i++) {
            currentDate = currentDate.plusMonths(1);
            String monthLabel = currentDate.format(MONTH_FORMAT);

            // Sazonalidade Simulada (Dezembro e Novembro têm pico de 1.3x)
            BigDecimal seasonalMultiplier = BigDecimal.ONE;
            int monthValue = currentDate.getMonthValue();
            if (monthValue == 11 || monthValue == 12) {
                seasonalMultiplier = BigDecimal.valueOf(1.3);
            } else if (monthValue == 1 || monthValue == 2) {
                seasonalMultiplier = BigDecimal.valueOf(0.9); // Baixa no começo de ano
            }

            // Aplicar crescimento e cenários ao volume
            BigDecimal projectedVolumeDecimal = currentVolume
                    .multiply(monthlyGrowthMultiplier.pow(i))
                    .multiply(seasonalMultiplier)
                    .multiply(scenarioMultiplier);

            int projectedVolume = projectedVolumeDecimal.intValue();

            BigDecimal projectedRevenue = currentPrice.multiply(BigDecimal.valueOf(projectedVolume));
            BigDecimal projectedCosts = currentCost.multiply(BigDecimal.valueOf(projectedVolume));
            
            // Custo Fixo Simulado (20% da receita inicial para dar peso)
            BigDecimal fixedCost = request.getBasePrice().multiply(BigDecimal.valueOf(request.getBaseVolume())).multiply(BigDecimal.valueOf(0.2));
            projectedCosts = projectedCosts.add(fixedCost);

            BigDecimal profit = projectedRevenue.subtract(projectedCosts);
            totalProfit = totalProfit.add(profit);

            projections.add(MonthlyProjection.builder()
                    .monthLabel(monthLabel)
                    .revenue(projectedRevenue.setScale(2, RoundingMode.HALF_UP))
                    .costs(projectedCosts.setScale(2, RoundingMode.HALF_UP))
                    .profit(profit.setScale(2, RoundingMode.HALF_UP))
                    .volume(projectedVolume)
                    .build());
        }

        // Calcula margem média
        BigDecimal averageMargin = BigDecimal.ZERO;
        if (totalProfit.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal totalRevenue = projections.stream().map(MonthlyProjection::getRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
                averageMargin = totalProfit.divide(totalRevenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            }
        }

        return ScenarioResult.builder()
                .scenarioName(name)
                .projections(projections)
                .totalAnnualProfit(totalProfit.setScale(2, RoundingMode.HALF_UP))
                .averageMargin(averageMargin.setScale(2, RoundingMode.HALF_UP))
                .build();
    }

    // =========================================================================
    // MODALIDADE 2: PREVISÃO COM ML (Fase 5 — usa histórico real)
    // =========================================================================

    /**
     * Previsão baseada em ML usando histórico real de transações.
     * 
     * Workflow:
     * 1. Busca transações do usuário via CoreServiceClient
     * 2. Agrega receitas por mês para formar a série temporal
     * 3. Se >= 6 meses de dados → ModelSelectorService escolhe o melhor algoritmo
     * 4. Se < 6 meses → usa regressão linear simples como fallback
     * 5. Aplica fator de correção do feedback
     * 6. Gera cenários (pessimista, neutro, otimista)
     * 7. Persiste as predições no banco
     * 
     * @param userId      ID do usuário
     * @param transactions Transações históricas do usuário
     * @param horizon     Meses a prever (padrão: 12)
     * @return ForecastResponse com cenários e metadados ML
     */
    @Transactional
    public ForecastResponse forecastWithML(Long userId, List<TransactionDTO> transactions, int horizon) {
        log.info("Iniciando previsão ML para usuário {} com {} transações, horizonte={}",
                userId, transactions.size(), horizon);

        // 1. Agregar receitas mensais
        double[] monthlyRevenue = aggregateMonthlyRevenue(transactions);
        log.info("Série temporal construída: {} meses de dados", monthlyRevenue.length);

        // 2. Selecionar e executar o melhor modelo
        ForecastResult forecastResult;
        if (monthlyRevenue.length >= MIN_MONTHS_FOR_ML) {
            forecastResult = modelSelectorService.selectBestModel(monthlyRevenue, horizon);
        } else {
            log.info("Dados insuficientes para ML ({} < {}). Usando regressão linear básica.",
                    monthlyRevenue.length, MIN_MONTHS_FOR_ML);
            forecastResult = modelSelectorService.selectBestModel(monthlyRevenue, horizon);
        }

        // 3. Aplicar fator de correção do feedback
        BigDecimal correctionFactor = feedbackService.getCurrentCorrectionFactor(userId);
        double corrFactor = correctionFactor.doubleValue();

        double[] correctedValues = Arrays.stream(forecastResult.getForecastValues())
                .map(v -> v * corrFactor)
                .toArray();

        forecastResult.setForecastValues(correctedValues);
        forecastResult.setCorrectionFactor(corrFactor);

        // 4. Gerar cenários
        ScenarioResult neutral = buildScenarioFromForecast(correctedValues, "NEUTRAL", 1.00);
        ScenarioResult optimist = buildScenarioFromForecast(correctedValues, "OPTIMIST", 1.15);
        ScenarioResult pessimist = buildScenarioFromForecast(correctedValues, "PESSIMIST", 0.85);

        // 5. Estimar custos baseados na proporção histórica
        double costRatio = estimateCostRatio(transactions);
        applyCostsToScenario(neutral, costRatio);
        applyCostsToScenario(optimist, costRatio);
        applyCostsToScenario(pessimist, costRatio);

        // 6. Persistir predições
        savePredictions(userId, neutral, Scenario.NEUTRAL, forecastResult);
        savePredictions(userId, optimist, Scenario.OPTIMIST, forecastResult);
        savePredictions(userId, pessimist, Scenario.PESSIMIST, forecastResult);

        // 7. Montar resposta
        Map<String, Double> modelParams = forecastResult.getParameters() != null
                ? forecastResult.getParameters()
                : Map.of();

        return ForecastResponse.builder()
                .neutral(neutral)
                .optimist(optimist)
                .pessimist(pessimist)
                .algorithmUsed(forecastResult.getAlgorithmUsed())
                .mape(BigDecimal.valueOf(forecastResult.getMape()).setScale(2, RoundingMode.HALF_UP))
                .confidenceScore(BigDecimal.valueOf(forecastResult.getConfidenceScore()).setScale(1, RoundingMode.HALF_UP))
                .qualityClassification(forecastResult.getQualityClassification())
                .correctionFactor(correctionFactor)
                .modelParameters(modelParams)
                .historyMonths(monthlyRevenue.length)
                .build();
    }

    /**
     * Agrega transações de receita por mês, formando a série temporal.
     * Retorna um array ordenado cronologicamente (mês mais antigo primeiro).
     */
    private double[] aggregateMonthlyRevenue(List<TransactionDTO> transactions) {
        // Filtrar apenas receitas e agrupar por mês
        Map<String, Double> monthlyMap = new TreeMap<>(); // TreeMap para ordenação natural

        transactions.stream()
                .filter(t -> "REVENUE".equals(t.getType()))
                .forEach(t -> {
                    String key = t.getDate().getYear() + "-" + String.format("%02d", t.getDate().getMonthValue());
                    monthlyMap.merge(key, t.getAmount().doubleValue(), Double::sum);
                });

        if (monthlyMap.isEmpty()) {
            return new double[0];
        }

        // Preencher meses vazios com zero (para manter continuidade da série)
        List<String> allMonths = new ArrayList<>(monthlyMap.keySet());
        String firstMonth = allMonths.get(0);
        String lastMonth = allMonths.get(allMonths.size() - 1);

        LocalDate start = LocalDate.parse(firstMonth + "-01");
        LocalDate end = LocalDate.parse(lastMonth + "-01");

        List<Double> values = new ArrayList<>();
        LocalDate current = start;
        while (!current.isAfter(end)) {
            String key = current.getYear() + "-" + String.format("%02d", current.getMonthValue());
            values.add(monthlyMap.getOrDefault(key, 0.0));
            current = current.plusMonths(1);
        }

        return values.stream().mapToDouble(Double::doubleValue).toArray();
    }

    /**
     * Estima a proporção de custos em relação à receita baseada no histórico.
     */
    private double estimateCostRatio(List<TransactionDTO> transactions) {
        double totalRevenue = transactions.stream()
                .filter(t -> "REVENUE".equals(t.getType()))
                .mapToDouble(t -> t.getAmount().doubleValue())
                .sum();

        double totalCosts = transactions.stream()
                .filter(t -> "FIXED_COST".equals(t.getType()) || "VARIABLE_COST".equals(t.getType()))
                .mapToDouble(t -> t.getAmount().doubleValue())
                .sum();

        if (totalRevenue > 0) {
            return totalCosts / totalRevenue;
        }
        return 0.6; // Default: 60% de custos
    }

    /**
     * Constrói um ScenarioResult a partir dos valores previstos pelo ML.
     */
    private ScenarioResult buildScenarioFromForecast(double[] forecastValues, String scenarioName, double multiplier) {
        List<MonthlyProjection> projections = new ArrayList<>();
        LocalDate currentDate = LocalDate.now();
        BigDecimal totalProfit = BigDecimal.ZERO;

        int horizonSize = Math.min(forecastValues.length, FORECAST_HORIZON);

        for (int i = 0; i < horizonSize; i++) {
            currentDate = i == 0 ? currentDate.plusMonths(1) : currentDate.plusMonths(1);
            String monthLabel = currentDate.format(MONTH_FORMAT);

            BigDecimal revenue = BigDecimal.valueOf(forecastValues[i] * multiplier)
                    .setScale(2, RoundingMode.HALF_UP);

            projections.add(MonthlyProjection.builder()
                    .monthLabel(monthLabel)
                    .revenue(revenue)
                    .costs(BigDecimal.ZERO) // Preenchido depois por applyCostsToScenario
                    .profit(BigDecimal.ZERO)
                    .volume(0) // ML não projeta volume individualmente
                    .build());
        }

        return ScenarioResult.builder()
                .scenarioName(scenarioName)
                .projections(projections)
                .totalAnnualProfit(BigDecimal.ZERO)
                .averageMargin(BigDecimal.ZERO)
                .build();
    }

    /**
     * Aplica estimativa de custos a um cenário baseado na proporção histórica.
     * Recalcula lucro e margem média.
     */
    private void applyCostsToScenario(ScenarioResult scenario, double costRatio) {
        BigDecimal totalProfit = BigDecimal.ZERO;
        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (MonthlyProjection proj : scenario.getProjections()) {
            BigDecimal costs = proj.getRevenue().multiply(BigDecimal.valueOf(costRatio))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal profit = proj.getRevenue().subtract(costs);

            proj.setCosts(costs);
            proj.setProfit(profit);

            totalProfit = totalProfit.add(profit);
            totalRevenue = totalRevenue.add(proj.getRevenue());
        }

        scenario.setTotalAnnualProfit(totalProfit.setScale(2, RoundingMode.HALF_UP));

        if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal margin = totalProfit.divide(totalRevenue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            scenario.setAverageMargin(margin);
        }
    }

    /**
     * Persiste as predições de um cenário no banco de dados.
     */
    private void savePredictions(Long userId, ScenarioResult scenario, Scenario scenarioEnum, ForecastResult forecastResult) {
        LocalDate currentDate = LocalDate.now();

        for (int i = 0; i < scenario.getProjections().size(); i++) {
            MonthlyProjection proj = scenario.getProjections().get(i);
            LocalDate predMonth = currentDate.plusMonths(i + 1).withDayOfMonth(1);

            // Serializar parâmetros do modelo como JSON
            String paramsJson = "";
            try {
                Map<String, Object> allParams = new HashMap<>();
                allParams.put("algorithm", forecastResult.getAlgorithmUsed());
                allParams.put("mape", forecastResult.getMape());
                allParams.put("correctionFactor", forecastResult.getCorrectionFactor());
                if (forecastResult.getParameters() != null) {
                    allParams.putAll(forecastResult.getParameters());
                }
                paramsJson = objectMapper.writeValueAsString(allParams);
            } catch (Exception e) {
                log.warn("Erro ao serializar parâmetros: {}", e.getMessage());
                paramsJson = "{\"algorithm\":\"" + forecastResult.getAlgorithmUsed() + "\"}";
            }

            PredictionResult predictionResult = PredictionResult.builder()
                    .userId(userId)
                    .month(predMonth)
                    .predictedRevenue(proj.getRevenue())
                    .scenario(scenarioEnum)
                    .confidenceScore(BigDecimal.valueOf(forecastResult.getConfidenceScore())
                            .setScale(2, RoundingMode.HALF_UP))
                    .parameters(paramsJson)
                    .build();

            predictionResultRepository.save(predictionResult);
        }

        log.debug("Salvas {} predições para cenário {} do usuário {}", 
                scenario.getProjections().size(), scenarioEnum, userId);
    }
}
