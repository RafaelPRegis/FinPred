package com.finpred.prediction.controller;

import com.finpred.prediction.dto.*;
import com.finpred.prediction.service.FeedbackService;
import com.finpred.prediction.service.PredictionEngineService;
import com.finpred.prediction.service.PredictionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller do Prediction Service.
 * 
 * Endpoints:
 * - POST /api/predict/simulate     → Simulação paramétrica (sliders)
 * - POST /api/predict/forecast     → Previsão ML com histórico real
 * - POST /api/predict/feedback     → Submeter valor realizado
 * - GET  /api/predict/feedback/history → Histórico de feedbacks
 * - GET  /api/predict/accuracy     → MAPE + classificação do modelo
 * - GET  /api/predict/history      → Histórico de predições salvas
 */
@RestController
@RequestMapping("/api/predict")
@RequiredArgsConstructor
@Slf4j
public class PredictionController {

    private final PredictionEngineService predictionEngineService;
    private final PredictionService predictionService;
    private final FeedbackService feedbackService;

    // =========================================================================
    // SIMULAÇÃO PARAMÉTRICA (Fase 4 — preservada)
    // =========================================================================

    /**
     * Simulação com parâmetros do usuário (preço, custo, volume, crescimento).
     * Chamada pelo SimulatorPage em tempo real ao mover sliders.
     */
    @PostMapping("/simulate")
    public ResponseEntity<SimulationResponse> simulate(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody SimulationRequest request) {
        
        SimulationResponse response = predictionEngineService.simulate(userId, request);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // PREVISÃO ML (Fase 5 — novo)
    // =========================================================================

    /**
     * Previsão baseada em Machine Learning usando histórico real de transações.
     * 
     * O ModelSelectorService escolhe automaticamente o melhor algoritmo
     * (ARMA, Holt-Winters ou Regressão Linear) baseado no MAPE de cada um.
     * 
     * Requer mínimo de 6 meses de dados para ativar ML avançado.
     */
    @PostMapping("/forecast")
    public ResponseEntity<ForecastResponse> forecast(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) ForecastRequest request) {

        if (request == null) {
            request = ForecastRequest.builder().horizon(12).build();
        }

        log.info("Requisição de previsão ML — userId={}, productId={}, horizonte={}",
                userId, request.getProductId(), request.getHorizon());

        ForecastResponse response = predictionService.predict(userId, authHeader, request);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // FEEDBACK (Fase 5 — novo)
    // =========================================================================

    /**
     * Submete feedback: valor real observado vs previsão.
     * 
     * Isso alimenta o cálculo do MAPE e o fator de correção automático,
     * que será aplicado às próximas previsões.
     */
    @PostMapping("/feedback")
    public ResponseEntity<FeedbackResponse> submitFeedback(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody FeedbackRequest request) {

        log.info("Feedback recebido — userId={}, predictionId={}, mês={}",
                userId, request.getPredictionId(), request.getMonth());

        FeedbackResponse response = feedbackService.submitFeedback(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Retorna o histórico de feedbacks do usuário.
     * Ordenado do mais recente para o mais antigo.
     */
    @GetMapping("/feedback/history")
    public ResponseEntity<List<FeedbackResponse>> getFeedbackHistory(
            @RequestHeader("X-User-Id") Long userId) {

        List<FeedbackResponse> history = feedbackService.getFeedbackHistory(userId);
        return ResponseEntity.ok(history);
    }

    // =========================================================================
    // ACURÁCIA DO MODELO (Fase 5 — novo)
    // =========================================================================

    /**
     * Retorna a acurácia geral do modelo de predição do usuário.
     * 
     * Inclui: MAPE, classificação textual, total de feedbacks,
     * fator de correção e melhor algoritmo em uso.
     */
    @GetMapping("/accuracy")
    public ResponseEntity<ModelAccuracyResponse> getModelAccuracy(
            @RequestHeader("X-User-Id") Long userId) {

        ModelAccuracyResponse accuracy = feedbackService.getModelAccuracy(userId);
        return ResponseEntity.ok(accuracy);
    }

    // =========================================================================
    // HISTÓRICO DE PREDIÇÕES (Fase 5 — novo)
    // =========================================================================

    /**
     * Retorna o histórico de predições salvas do usuário.
     * Mostra apenas cenário neutro para cada mês previsto.
     */
    @GetMapping("/history")
    public ResponseEntity<List<PredictionHistoryItem>> getPredictionHistory(
            @RequestHeader("X-User-Id") Long userId) {

        List<PredictionHistoryItem> history = predictionService.getHistory(userId);
        return ResponseEntity.ok(history);
    }
}
