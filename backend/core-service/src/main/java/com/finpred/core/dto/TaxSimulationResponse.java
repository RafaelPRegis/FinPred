package com.finpred.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxSimulationResponse {

    private BigDecimal monthlyRevenue;
    private BigDecimal estimatedTax;
    private BigDecimal effectiveRate;
    private String appliedRule;
    private Map<String, BigDecimal> taxBreakdown;
}
