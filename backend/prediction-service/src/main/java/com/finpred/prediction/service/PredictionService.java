package com.finpred.prediction.service;

import com.finpred.prediction.repository.FeedbackLogRepository;
import com.finpred.prediction.repository.PredictionResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Serviço de predição financeira.
 * Motor preditivo, simulador e feedback serão implementados nas Fases 4 e 5.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PredictionService {

    private final PredictionResultRepository predictionResultRepository;
    private final FeedbackLogRepository feedbackLogRepository;

    // TODO: Fase 4 — predict(), simulate(), calculateMAPE(), calculateSeasonality()
}
