package com.finpred.prediction.service;

import com.finpred.prediction.dto.MonthlyProjection;
import com.finpred.prediction.dto.ScenarioResult;
import com.finpred.prediction.dto.SimulationRequest;
import com.finpred.prediction.dto.SimulationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PredictionEngineService {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMM/yyyy");

    public SimulationResponse simulate(SimulationRequest request) {
        log.info("Processando simulação: {}", request);

        ScenarioResult neutral = generateScenario(request, "NEUTRAL", BigDecimal.valueOf(1.0));
        ScenarioResult optimist = generateScenario(request, "OPTIMIST", BigDecimal.valueOf(1.15));
        ScenarioResult pessimist = generateScenario(request, "PESSIMIST", BigDecimal.valueOf(0.85));

        return SimulationResponse.builder()
                .neutral(neutral)
                .optimist(optimist)
                .pessimist(pessimist)
                .build();
    }

    private ScenarioResult generateScenario(SimulationRequest request, String name, BigDecimal scenarioMultiplier) {
        List<MonthlyProjection> projections = new ArrayList<>();
        LocalDate currentDate = LocalDate.now();
        BigDecimal totalProfit = BigDecimal.ZERO;

        BigDecimal currentPrice = request.getBasePrice();
        BigDecimal currentCost = request.getBaseCost();
        BigDecimal currentVolume = BigDecimal.valueOf(request.getBaseVolume());

        // A taxa de crescimento esperada (ex: 5%) é convertida em multiplicador mensal
        // Se a taxa for 5% anual, mensal = 5 / 12 = 0.41%
        BigDecimal annualGrowthRate = request.getExpectedGrowthRate().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal monthlyGrowthMultiplier = BigDecimal.ONE.add(annualGrowthRate.divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP));

        for (int i = 1; i <= 12; i++) {
            currentDate = currentDate.plusMonths(1);
            String monthLabel = currentDate.format(MONTH_FORMAT);

            // Sazonalidade Simulada (Dezembro e Novembro têm pico de 1.3x)
            BigDecimal seasonalMultiplier = BigDecimal.ONE;
            int monthValue = currentDate.getMonthValue();
            if (monthValue == 11 || monthValue == 12) {
                seasonalMultiplier = BigDecimal.valueOf(1.3);
            } else if (monthValue == 1 || monthValue == 2) {
                seasonalMultiplier = BigDecimal.valueOf(0.9); // Baixa no começo de ano
            }

            // Aplicar crescimento e cenários ao volume
            BigDecimal projectedVolumeDecimal = currentVolume
                    .multiply(monthlyGrowthMultiplier.pow(i))
                    .multiply(seasonalMultiplier)
                    .multiply(scenarioMultiplier);

            int projectedVolume = projectedVolumeDecimal.intValue();

            BigDecimal projectedRevenue = currentPrice.multiply(BigDecimal.valueOf(projectedVolume));
            BigDecimal projectedCosts = currentCost.multiply(BigDecimal.valueOf(projectedVolume));
            
            // Custo Fixo Simulado (20% da receita inicial para dar peso)
            BigDecimal fixedCost = request.getBasePrice().multiply(BigDecimal.valueOf(request.getBaseVolume())).multiply(BigDecimal.valueOf(0.2));
            projectedCosts = projectedCosts.add(fixedCost);

            BigDecimal profit = projectedRevenue.subtract(projectedCosts);
            totalProfit = totalProfit.add(profit);

            projections.add(MonthlyProjection.builder()
                    .monthLabel(monthLabel)
                    .revenue(projectedRevenue.setScale(2, RoundingMode.HALF_UP))
                    .costs(projectedCosts.setScale(2, RoundingMode.HALF_UP))
                    .profit(profit.setScale(2, RoundingMode.HALF_UP))
                    .volume(projectedVolume)
                    .build());
        }

        // Calcula margem média
        BigDecimal averageMargin = BigDecimal.ZERO;
        if (totalProfit.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal totalRevenue = projections.stream().map(MonthlyProjection::getRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
                averageMargin = totalProfit.divide(totalRevenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            }
        }

        return ScenarioResult.builder()
                .scenarioName(name)
                .projections(projections)
                .totalAnnualProfit(totalProfit.setScale(2, RoundingMode.HALF_UP))
                .averageMargin(averageMargin.setScale(2, RoundingMode.HALF_UP))
                .build();
    }
}
