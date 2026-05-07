package com.finpred.core.service;

import com.finpred.core.dto.AlertDTO;
import com.finpred.core.dto.DashboardSummary;
import com.finpred.core.model.Transaction;
import com.finpred.core.model.TransactionType;
import com.finpred.core.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço do Dashboard — integra dados reais com alertas inteligentes.
 * 
 * Quando o usuário tem transações suficientes, usa dados reais.
 * Caso contrário, retorna dados demonstrativos para o primeiro acesso.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final AlertService alertService;

    public DashboardSummary getSummary(Long userId) {
        List<Transaction> transactions = transactionRepository.findByUserId(userId);

        // Se o usuário não tem dados, retorna mock demonstrativo
        if (transactions.isEmpty()) {
            return buildDemoSummary(userId);
        }

        return buildRealSummary(userId, transactions);
    }

    /**
     * Constrói o sumário com dados reais do usuário.
     */
    private DashboardSummary buildRealSummary(Long userId, List<Transaction> transactions) {
        // Calcular KPIs reais
        LocalDate now = LocalDate.now();
        LocalDate threeMonthsAgo = now.minusMonths(3);

        // Receitas e custos dos últimos 3 meses
        BigDecimal recentRevenue = transactions.stream()
                .filter(t -> t.getType() == TransactionType.REVENUE && t.getDate().isAfter(threeMonthsAgo))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal recentCosts = transactions.stream()
                .filter(t -> (t.getType() == TransactionType.FIXED_COST || t.getType() == TransactionType.VARIABLE_COST)
                        && t.getDate().isAfter(threeMonthsAgo))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal recentProfit = recentRevenue.subtract(recentCosts);
        BigDecimal averageMargin = BigDecimal.ZERO;
        if (recentRevenue.compareTo(BigDecimal.ZERO) > 0) {
            averageMargin = recentProfit.divide(recentRevenue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(1, RoundingMode.HALF_UP);
        }

        // Receita projetada (média mensal × 3 projetada para frente)
        BigDecimal monthlyAvgRevenue = recentRevenue.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
        BigDecimal projectedRevenue = monthlyAvgRevenue.multiply(BigDecimal.valueOf(3));

        // Risco de caixa
        String cashRisk;
        String riskSubtext;
        if (recentProfit.compareTo(BigDecimal.ZERO) < 0) {
            cashRisk = "Alto";
            riskSubtext = "Despesas superam receitas";
        } else if (averageMargin.compareTo(new BigDecimal("15")) < 0) {
            cashRisk = "Moderado";
            riskSubtext = "Margem apertada";
        } else {
            cashRisk = "Baixo";
            riskSubtext = "Saudável";
        }

        // Gráfico Previsto vs Realizado — dados mensais reais
        List<DashboardSummary.ChartPoint> predictedVsActual = buildPredictedVsActual(transactions);

        // Gráfico de Cenários Futuros
        List<DashboardSummary.ScenarioPoint> futureScenarios = buildFutureScenarios(monthlyAvgRevenue);

        // Alertas inteligentes para insight do dia
        List<AlertDTO> alerts = alertService.generateAlerts(userId);
        String dailyInsight = alerts.isEmpty()
                ? "Continue registrando transações para obter insights mais precisos."
                : alerts.get(0).getMessage();

        return DashboardSummary.builder()
                .projectedRevenue(projectedRevenue)
                .revenueSubtext("Baseado na média dos últimos 3 meses")
                .projectedProfit(recentProfit)
                .profitSubtext(recentProfit.compareTo(BigDecimal.ZERO) > 0 ? "Positivo" : "Negativo — atenção!")
                .averageMargin(averageMargin)
                .marginSubtext(averageMargin.compareTo(new BigDecimal("20")) >= 0 ? "Saudável" : "Abaixo do ideal")
                .cashRisk(cashRisk)
                .riskSubtext(riskSubtext)
                .dailyInsight(dailyInsight)
                .predictedVsActual(predictedVsActual)
                .futureScenarios(futureScenarios)
                .build();
    }

    /**
     * Constrói dados do gráfico Previsto vs Realizado.
     * Agrupa transações de receita por mês.
     */
    private List<DashboardSummary.ChartPoint> buildPredictedVsActual(List<Transaction> transactions) {
        // Agrupar receitas reais por mês
        Map<String, BigDecimal> monthlyActual = transactions.stream()
                .filter(t -> t.getType() == TransactionType.REVENUE)
                .collect(Collectors.groupingBy(
                        t -> {
                            Locale locale = new Locale("pt", "BR");
                            String monthName = t.getDate().getMonth().getDisplayName(TextStyle.SHORT, locale);
                            return monthName.substring(0, 1).toUpperCase() + monthName.substring(1);
                        },
                        LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        List<DashboardSummary.ChartPoint> points = new ArrayList<>();
        BigDecimal prevValue = null;

        for (Map.Entry<String, BigDecimal> entry : monthlyActual.entrySet()) {
            BigDecimal actual = entry.getValue();
            // "Previsto" é o valor do mês anterior como baseline simples
            BigDecimal predicted = prevValue != null
                    ? prevValue.multiply(BigDecimal.valueOf(1.05)).setScale(2, RoundingMode.HALF_UP)
                    : actual.multiply(BigDecimal.valueOf(0.95)).setScale(2, RoundingMode.HALF_UP);

            points.add(new DashboardSummary.ChartPoint(entry.getKey(), actual, predicted));
            prevValue = actual;
        }

        // Limitar aos últimos 6 meses
        if (points.size() > 6) {
            points = points.subList(points.size() - 6, points.size());
        }

        return points;
    }

    /**
     * Constrói cenários futuros baseados na média mensal de receita.
     */
    private List<DashboardSummary.ScenarioPoint> buildFutureScenarios(BigDecimal monthlyAvgRevenue) {
        List<DashboardSummary.ScenarioPoint> scenarios = new ArrayList<>();

        for (int i = 1; i <= 6; i++) {
            BigDecimal growth = BigDecimal.ONE.add(BigDecimal.valueOf(0.03 * i)); // 3% crescimento mensal
            BigDecimal optimistic = monthlyAvgRevenue.multiply(growth).multiply(BigDecimal.valueOf(1.15))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal neutral = monthlyAvgRevenue.multiply(growth)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal pessimistic = monthlyAvgRevenue.multiply(growth).multiply(BigDecimal.valueOf(0.85))
                    .setScale(2, RoundingMode.HALF_UP);

            scenarios.add(new DashboardSummary.ScenarioPoint("Proj " + i, optimistic, neutral, pessimistic));
        }

        return scenarios;
    }

    /**
     * Sumário demonstrativo para novos usuários sem dados.
     */
    private DashboardSummary buildDemoSummary(Long userId) {
        return DashboardSummary.builder()
                .projectedRevenue(new BigDecimal("152400.00"))
                .revenueSubtext("Dados demonstrativos — importe transações")
                .projectedProfit(new BigDecimal("42800.00"))
                .profitSubtext("Exemplo — 0% vs mês anterior")
                .averageMargin(new BigDecimal("28.1"))
                .marginSubtext("Exemplo — Estável")
                .cashRisk("Baixo")
                .riskSubtext("Dados demonstrativos")
                .dailyInsight("Importe suas transações ou cadastre produtos para começar a receber insights reais.")
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
