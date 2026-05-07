package com.finpred.prediction.service;

import com.finpred.prediction.dto.FeedbackRequest;
import com.finpred.prediction.dto.FeedbackResponse;
import com.finpred.prediction.dto.ModelAccuracyResponse;
import com.finpred.prediction.engine.TimeSeriesUtils;
import com.finpred.prediction.model.FeedbackLog;
import com.finpred.prediction.model.PredictionResult;
import com.finpred.prediction.model.Scenario;
import com.finpred.prediction.repository.FeedbackLogRepository;
import com.finpred.prediction.repository.PredictionResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Serviço de Feedback — compara previsões com valores reais.
 * 
 * Responsável por:
 * 1. Registrar feedback (valor real vs previsão)
 * 2. Calcular MAPE global do usuário
 * 3. Calcular fator de correção automático
 * 4. Fornecer histórico de feedbacks com métricas de erro
 * 5. Classificar a precisão do modelo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackLogRepository feedbackLogRepository;
    private final PredictionResultRepository predictionResultRepository;

    /**
     * Submete um feedback comparando o valor real com a previsão.
     * 
     * Workflow:
     * 1. Busca a predição original
     * 2. Calcula o erro percentual absoluto
     * 3. Recalcula o MAPE global do usuário
     * 4. Recalcula o fator de correção
     * 5. Persiste o FeedbackLog
     * 
     * @param userId  ID do usuário
     * @param request Dados do feedback
     * @return FeedbackResponse com métricas calculadas
     */
    @Transactional
    public FeedbackResponse submitFeedback(Long userId, FeedbackRequest request) {
        log.info("Recebendo feedback do usuário {} para predição {} (mês: {})",
                userId, request.getPredictionId(), request.getMonth());

        // 1. Buscar a predição original
        PredictionResult prediction = predictionResultRepository.findById(request.getPredictionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Predição não encontrada: " + request.getPredictionId()));

        BigDecimal predictedValue = prediction.getPredictedRevenue();
        BigDecimal actualValue = request.getActualValue();

        // 2. Calcular erro percentual absoluto: |actual - predicted| / |actual| * 100
        BigDecimal errorPercentage = BigDecimal.ZERO;
        if (actualValue.compareTo(BigDecimal.ZERO) != 0) {
            errorPercentage = actualValue.subtract(predictedValue).abs()
                    .divide(actualValue.abs(), 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(4, RoundingMode.HALF_UP);
        }

        // 3. Criar e salvar o FeedbackLog
        FeedbackLog feedbackLog = FeedbackLog.builder()
                .userId(userId)
                .predictionId(request.getPredictionId())
                .month(request.getMonth())
                .actualValue(actualValue)
                .predictedValue(predictedValue)
                .errorPercentage(errorPercentage)
                .build();

        // 4. Calcular MAPE global atualizado
        BigDecimal mape = calculateMAPE(userId, feedbackLog);
        feedbackLog.setMapeScore(mape);

        // 5. Calcular fator de correção atualizado
        BigDecimal correctionFactor = calculateCorrectionFactor(userId, feedbackLog);
        feedbackLog.setCorrectionFactor(correctionFactor);

        feedbackLog = feedbackLogRepository.save(feedbackLog);

        log.info("Feedback registrado: erro={:.2f}%, MAPE global={:.2f}%, correção={:.4f}",
                errorPercentage, mape, correctionFactor);

        return FeedbackResponse.builder()
                .id(feedbackLog.getId())
                .predictionId(request.getPredictionId())
                .month(request.getMonth())
                .predictedValue(predictedValue)
                .actualValue(actualValue)
                .errorPercentage(errorPercentage)
                .mapeScore(mape)
                .correctionFactor(correctionFactor)
                .createdAt(feedbackLog.getCreatedAt())
                .build();
    }

    /**
     * Calcula o MAPE global do usuário considerando todos os feedbacks
     * existentes + o novo feedback sendo registrado.
     */
    private BigDecimal calculateMAPE(Long userId, FeedbackLog newFeedback) {
        List<FeedbackLog> existingFeedbacks = feedbackLogRepository.findByUserId(userId);

        // Incluir o novo feedback no cálculo
        int totalCount = existingFeedbacks.size() + 1;
        double[] actuals = new double[totalCount];
        double[] predicteds = new double[totalCount];

        for (int i = 0; i < existingFeedbacks.size(); i++) {
            actuals[i] = existingFeedbacks.get(i).getActualValue().doubleValue();
            predicteds[i] = existingFeedbacks.get(i).getPredictedValue().doubleValue();
        }
        actuals[totalCount - 1] = newFeedback.getActualValue().doubleValue();
        predicteds[totalCount - 1] = newFeedback.getPredictedValue().doubleValue();

        double mape = TimeSeriesUtils.calculateMAPE(actuals, predicteds);
        return BigDecimal.valueOf(mape).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Calcula o fator de correção automático baseado no viés médio.
     * 
     * F_correção = 1 + Média(Ai - Fi) / |Média(Fi)|
     * 
     * Se o modelo subestima → fator > 1.0
     * Se o modelo superestima → fator < 1.0
     */
    private BigDecimal calculateCorrectionFactor(Long userId, FeedbackLog newFeedback) {
        List<FeedbackLog> existingFeedbacks = feedbackLogRepository.findByUserId(userId);

        int totalCount = existingFeedbacks.size() + 1;
        double[] actuals = new double[totalCount];
        double[] predicteds = new double[totalCount];

        for (int i = 0; i < existingFeedbacks.size(); i++) {
            actuals[i] = existingFeedbacks.get(i).getActualValue().doubleValue();
            predicteds[i] = existingFeedbacks.get(i).getPredictedValue().doubleValue();
        }
        actuals[totalCount - 1] = newFeedback.getActualValue().doubleValue();
        predicteds[totalCount - 1] = newFeedback.getPredictedValue().doubleValue();

        double factor = TimeSeriesUtils.calculateCorrectionFactor(actuals, predicteds);
        return BigDecimal.valueOf(factor).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Retorna o histórico completo de feedbacks do usuário,
     * ordenado do mais recente para o mais antigo.
     */
    public List<FeedbackResponse> getFeedbackHistory(Long userId) {
        return feedbackLogRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Retorna a acurácia geral do modelo do usuário.
     * Inclui MAPE, classificação, total de feedbacks e fator de correção.
     */
    public ModelAccuracyResponse getModelAccuracy(Long userId) {
        long totalFeedbacks = feedbackLogRepository.countByUserId(userId);

        if (totalFeedbacks == 0) {
            return ModelAccuracyResponse.builder()
                    .mape(BigDecimal.ZERO)
                    .classification("Sem dados")
                    .classificationIcon("fas fa-question-circle")
                    .totalFeedbacks(0)
                    .correctionFactor(BigDecimal.ONE)
                    .bestAlgorithm("N/A")
                    .confidenceScore(BigDecimal.ZERO)
                    .build();
        }

        // MAPE médio calculado no banco
        Double avgError = feedbackLogRepository.findAverageErrorByUserId(userId);
        double mape = avgError != null ? avgError : 50.0;

        // Fator de correção mais recente
        BigDecimal correctionFactor = feedbackLogRepository.findLatestCorrectionFactorByUserId(userId);
        if (correctionFactor == null) correctionFactor = BigDecimal.ONE;

        // Classificação
        String classification = TimeSeriesUtils.classifyMAPEDisplay(mape);
        String icon = classificationIcon(mape);

        // Algoritmo mais usado (da predição mais recente)
        String bestAlgorithm = "LINEAR_REGRESSION"; // default
        List<PredictionResult> recentPredictions = predictionResultRepository
                .findByUserIdOrderByMonthDesc(userId);
        if (!recentPredictions.isEmpty() && recentPredictions.get(0).getParameters() != null) {
            String params = recentPredictions.get(0).getParameters();
            if (params.contains("ARMA")) bestAlgorithm = "ARMA";
            else if (params.contains("HOLT_WINTERS")) bestAlgorithm = "HOLT_WINTERS";
        }

        double confidenceScore = Math.max(0, Math.min(100, 100 - mape));

        return ModelAccuracyResponse.builder()
                .mape(BigDecimal.valueOf(mape).setScale(2, RoundingMode.HALF_UP))
                .classification(classification)
                .classificationIcon(icon)
                .totalFeedbacks(totalFeedbacks)
                .correctionFactor(correctionFactor)
                .bestAlgorithm(bestAlgorithm)
                .confidenceScore(BigDecimal.valueOf(confidenceScore).setScale(1, RoundingMode.HALF_UP))
                .build();
    }

    /**
     * Retorna o fator de correção atual do usuário.
     * Usado pelo PredictionEngineService para ajustar previsões.
     */
    public BigDecimal getCurrentCorrectionFactor(Long userId) {
        BigDecimal factor = feedbackLogRepository.findLatestCorrectionFactorByUserId(userId);
        return factor != null ? factor : BigDecimal.ONE;
    }

    /**
     * Retorna o ícone correspondente à classificação MAPE.
     */
    private String classificationIcon(double mape) {
        if (mape < 10.0) return "fas fa-check-circle";
        if (mape < 20.0) return "fas fa-thumbs-up";
        if (mape < 50.0) return "fas fa-exclamation-triangle";
        return "fas fa-times-circle";
    }

    private FeedbackResponse toResponse(FeedbackLog log) {
        return FeedbackResponse.builder()
                .id(log.getId())
                .predictionId(log.getPredictionId())
                .month(log.getMonth())
                .predictedValue(log.getPredictedValue())
                .actualValue(log.getActualValue())
                .errorPercentage(log.getErrorPercentage())
                .mapeScore(log.getMapeScore())
                .correctionFactor(log.getCorrectionFactor())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
