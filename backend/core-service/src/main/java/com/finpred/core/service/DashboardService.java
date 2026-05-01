package com.finpred.core.service;

import com.finpred.core.dto.DashboardSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    public DashboardSummary getSummary(Long userId) {
        // Mocks for Phase 2.2 UI. Real predictions will be hooked in Phase 4.
        return DashboardSummary.builder()
                .projectedRevenue(new BigDecimal("152400.00"))
                .revenueSubtext("Mantenha dados para análise")
                .projectedProfit(new BigDecimal("42800.00"))
                .profitSubtext("0% vs mês anterior")
                .averageMargin(new BigDecimal("28.1"))
                .marginSubtext("— Estável")
                .cashRisk("Baixo")
                .riskSubtext("Saudável")
                .dailyInsight("Aumento de 12% na margem do Produto A projetado para Junho.")
                .predictedVsActual(List.of(
                        new DashboardSummary.ChartPoint("Jan", new BigDecimal("120000"), new BigDecimal("114000")),
                        new DashboardSummary.ChartPoint("Fev", new BigDecimal("125000"), new BigDecimal("118000")),
                        new DashboardSummary.ChartPoint("Mar", new BigDecimal("140000"), new BigDecimal("133000")),
                        new DashboardSummary.ChartPoint("Abr", new BigDecimal("152400"), new BigDecimal("145000"))
                ))
                .futureScenarios(List.of(
                        new DashboardSummary.ScenarioPoint("Proj 1", new BigDecimal("175000"), new BigDecimal("160000"), new BigDecimal("145000")),
                        new DashboardSummary.ScenarioPoint("Proj 2", new BigDecimal("200000"), new BigDecimal("168000"), new BigDecimal("138000")),
                        new DashboardSummary.ScenarioPoint("Proj 3", new BigDecimal("230000"), new BigDecimal("175000"), new BigDecimal("130000")),
                        new DashboardSummary.ScenarioPoint("Proj 4", new BigDecimal("265000"), new BigDecimal("185000"), new BigDecimal("125000")),
                        new DashboardSummary.ScenarioPoint("Proj 5", new BigDecimal("310000"), new BigDecimal("192000"), new BigDecimal("120000")),
                        new DashboardSummary.ScenarioPoint("Proj 6", new BigDecimal("350000"), new BigDecimal("205000"), new BigDecimal("115000"))
                ))
                .build();
    }
}
