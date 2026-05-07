package com.finpred.prediction.repository;

import com.finpred.prediction.model.PredictionResult;
import com.finpred.prediction.model.Scenario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PredictionResultRepository extends JpaRepository<PredictionResult, Long> {

    List<PredictionResult> findByUserId(Long userId);

    List<PredictionResult> findByUserIdAndScenario(Long userId, Scenario scenario);

    /** Histórico de predições do usuário, mais recente primeiro */
    List<PredictionResult> findByUserIdOrderByMonthDesc(Long userId);

    /** Buscar predição específica por mês e cenário */
    Optional<PredictionResult> findTopByUserIdAndMonthAndScenario(Long userId, LocalDate month, Scenario scenario);

    /** Predições neutras (para comparação com feedback) */
    List<PredictionResult> findByUserIdAndScenarioOrderByMonthAsc(Long userId, Scenario scenario);
}
