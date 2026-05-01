package com.finpred.prediction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioResult {
    private String scenarioName; // PESSIMIST, NEUTRAL, OPTIMIST
    private List<MonthlyProjection> projections;
    private BigDecimal totalAnnualProfit;
    private BigDecimal averageMargin;
}
