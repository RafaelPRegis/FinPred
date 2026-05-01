package com.finpred.prediction.repository;

import com.finpred.prediction.model.FeedbackLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackLogRepository extends JpaRepository<FeedbackLog, Long> {
    List<FeedbackLog> findByPredictionId(Long predictionId);
}
