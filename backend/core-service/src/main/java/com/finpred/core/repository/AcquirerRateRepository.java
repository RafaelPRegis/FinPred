package com.finpred.core.repository;

import com.finpred.core.model.AcquirerRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcquirerRateRepository extends JpaRepository<AcquirerRate, Long> {
    List<AcquirerRate> findByUserIdAndActiveTrue(Long userId);
    List<AcquirerRate> findByUserId(Long userId);
}
