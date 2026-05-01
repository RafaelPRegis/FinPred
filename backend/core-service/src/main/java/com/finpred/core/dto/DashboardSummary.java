package com.finpred.core.dto;

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
public class DashboardSummary {

    // KPIs Projetivos
    private BigDecimal projectedRevenue;
    private BigDecimal projectedProfit;
    private BigDecimal averageMargin;
    private String cashRisk;
    
    // Variações e Textos dos KPIs
    private String revenueSubtext;
    private String profitSubtext;
    private String marginSubtext;
    private String riskSubtext;

    // Insight do dia (Sidebar)
    private String dailyInsight;

    // Gráfico 1: Faturamento Previsto vs Realizado
    private List<ChartPoint> predictedVsActual;

    // Gráfico 2: Cenários Futuros
    private List<ScenarioPoint> futureScenarios;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartPoint {
        private String label;
        private BigDecimal actual;
        private BigDecimal predicted;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScenarioPoint {
        private String label;
        private BigDecimal optimistic;
        private BigDecimal neutral;
        private BigDecimal pessimistic;
    }
}
