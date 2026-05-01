package com.finpred.prediction.repository;

import com.finpred.prediction.model.PredictionResult;
import com.finpred.prediction.model.Scenario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PredictionResultRepository extends JpaRepository<PredictionResult, Long> {
    List<PredictionResult> findByUserId(Long userId);
    List<PredictionResult> findByUserIdAndScenario(Long userId, Scenario scenario);
}
