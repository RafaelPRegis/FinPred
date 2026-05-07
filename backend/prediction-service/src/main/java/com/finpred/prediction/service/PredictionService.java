package com.finpred.prediction.service;

import com.finpred.prediction.dto.*;
import com.finpred.prediction.model.PredictionResult;
import com.finpred.prediction.model.Scenario;
import com.finpred.prediction.repository.FeedbackLogRepository;
import com.finpred.prediction.repository.PredictionResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Serviço de predição financeira — orquestra previsão e histórico.
 * 
 * Integra:
 * - CoreServiceClient para buscar transações reais
 * - PredictionEngineService para executar ML
 * - FeedbackService para fator de correção
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PredictionService {

    private final PredictionResultRepository predictionResultRepository;
    private final FeedbackLogRepository feedbackLogRepository;
    private final PredictionEngineService predictionEngineService;
    private final CoreServiceClient coreServiceClient;

    /**
     * Executa previsão completa baseada em ML usando dados reais do usuário.
     * 
     * @param userId  ID do usuário
     * @param token   Token de autenticação para buscar transações
     * @param request Parâmetros da previsão (productId, horizon)
     * @return ForecastResponse com cenários, MAPE e metadados
     */
    public ForecastResponse predict(Long userId, String token, ForecastRequest request) {
        log.info("Executando previsão ML para usuário {}, productId={}, horizonte={}",
                userId, request.getProductId(), request.getHorizon());

        // 1. Buscar transações reais do usuário
        List<TransactionDTO> transactions = coreServiceClient.getUserTransactions(userId, token);

        if (transactions.isEmpty()) {
            log.warn("Nenhuma transação encontrada para o usuário {}. Retornando previsão vazia.", userId);
            return buildEmptyForecast();
        }

        // 2. Filtrar por produto se necessário
        if (request.getProductId() != null) {
            transactions = transactions.stream()
                    .filter(t -> request.getProductId().equals(t.getProductId()))
                    .toList();

            if (transactions.isEmpty()) {
                log.warn("Nenhuma transação encontrada para o produto {}.", request.getProductId());
                return buildEmptyForecast();
            }
        }

        // 3. Executar previsão ML
        int horizon = request.getHorizon() > 0 ? request.getHorizon() : 12;
        return predictionEngineService.forecastWithML(userId, transactions, horizon);
    }

    /**
     * Retorna o histórico de predições salvas do usuário.
     * Agrupadas por mês, mostrando apenas o cenário neutro para simplicidade.
     */
    public List<PredictionHistoryItem> getHistory(Long userId) {
        List<PredictionResult> neutralPredictions = predictionResultRepository
                .findByUserIdAndScenarioOrderByMonthAsc(userId, Scenario.NEUTRAL);

        return neutralPredictions.stream()
                .map(p -> PredictionHistoryItem.builder()
                        .id(p.getId())
                        .month(p.getMonth())
                        .monthLabel(p.getMonth().format(DateTimeFormatter.ofPattern("MMM/yyyy")))
                        .predictedRevenue(p.getPredictedRevenue())
                        .confidenceScore(p.getConfidenceScore())
                        .algorithm(extractAlgorithm(p.getParameters()))
                        .createdAt(p.getCreatedAt())
                        .build())
                .toList();
    }

    /**
     * Extrai o nome do algoritmo do JSON de parâmetros.
     */
    private String extractAlgorithm(String parametersJson) {
        if (parametersJson == null || parametersJson.isEmpty()) {
            return "N/A";
        }
        // Busca simples no JSON sem parser completo
        if (parametersJson.contains("ARMA")) return "ARMA";
        if (parametersJson.contains("HOLT_WINTERS")) return "HOLT_WINTERS";
        if (parametersJson.contains("LINEAR_REGRESSION")) return "LINEAR_REGRESSION";
        return "N/A";
    }

    /**
     * Constrói uma resposta vazia quando não há dados para previsão.
     */
    private ForecastResponse buildEmptyForecast() {
        return ForecastResponse.builder()
                .neutral(ScenarioResult.builder()
                        .scenarioName("NEUTRAL")
                        .projections(List.of())
                        .totalAnnualProfit(BigDecimal.ZERO)
                        .averageMargin(BigDecimal.ZERO)
                        .build())
                .optimist(ScenarioResult.builder()
                        .scenarioName("OPTIMIST")
                        .projections(List.of())
                        .totalAnnualProfit(BigDecimal.ZERO)
                        .averageMargin(BigDecimal.ZERO)
                        .build())
                .pessimist(ScenarioResult.builder()
                        .scenarioName("PESSIMIST")
                        .projections(List.of())
                        .totalAnnualProfit(BigDecimal.ZERO)
                        .averageMargin(BigDecimal.ZERO)
                        .build())
                .algorithmUsed("NONE")
                .mape(BigDecimal.valueOf(100))
                .confidenceScore(BigDecimal.ZERO)
                .qualityClassification("Sem dados disponíveis")
                .correctionFactor(BigDecimal.ONE)
                .historyMonths(0)
                .build();
    }
}
