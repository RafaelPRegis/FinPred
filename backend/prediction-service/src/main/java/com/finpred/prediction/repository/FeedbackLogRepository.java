package com.finpred.prediction.repository;

import com.finpred.prediction.model.FeedbackLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackLogRepository extends JpaRepository<FeedbackLog, Long> {

    List<FeedbackLog> findByPredictionId(Long predictionId);

    /** Todos os feedbacks de um usuário, ordenados cronologicamente (mais recente primeiro) */
    List<FeedbackLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Todos os feedbacks de um usuário (sem ordem específica) */
    List<FeedbackLog> findByUserId(Long userId);

    /** Contagem total de feedbacks de um usuário */
    long countByUserId(Long userId);

    /** MAPE médio de um usuário (calculado no banco para eficiência) */
    @Query("SELECT AVG(f.errorPercentage) FROM FeedbackLog f WHERE f.userId = :userId AND f.errorPercentage IS NOT NULL")
    Double findAverageErrorByUserId(@Param("userId") Long userId);

    /** Último fator de correção registrado para um usuário */
    @Query("SELECT f.correctionFactor FROM FeedbackLog f WHERE f.userId = :userId ORDER BY f.createdAt DESC LIMIT 1")
    java.math.BigDecimal findLatestCorrectionFactorByUserId(@Param("userId") Long userId);
}
