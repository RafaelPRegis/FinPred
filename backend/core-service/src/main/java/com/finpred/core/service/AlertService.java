package com.finpred.core.service;

import com.finpred.core.dto.AlertDTO;
import com.finpred.core.model.Product;
import com.finpred.core.model.Transaction;
import com.finpred.core.model.TransactionType;
import com.finpred.core.repository.ProductRepository;
import com.finpred.core.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Sistema de Alertas Inteligentes.
 * 
 * Analisa dados financeiros do usuário e gera alertas proativos sobre:
 * 
 * 1. Margem baixa ou negativa em produtos
 * 2. Fluxo de caixa negativo
 * 3. Queda de receita vs média histórica
 * 4. Custos fixos crescentes
 * 5. Saúde financeira positiva
 * 6. Insuficiência de dados
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final ProductRepository productRepository;
    private final TransactionRepository transactionRepository;

    /** Limiar de margem considerada "baixa" */
    private static final BigDecimal LOW_MARGIN_THRESHOLD = new BigDecimal("15.00");

    /** Limiar de queda de receita vs média (80%) */
    private static final double REVENUE_DROP_THRESHOLD = 0.80;

    /** Limiar de aumento de custos (10%) */
    private static final double COST_INCREASE_THRESHOLD = 0.10;

    /** Margem mínima para considerar "saudável" */
    private static final BigDecimal HEALTHY_MARGIN_THRESHOLD = new BigDecimal("30.00");

    /**
     * Gera todos os alertas inteligentes para um usuário.
     * 
     * @param userId ID do usuário
     * @return Lista de alertas ordenada por severidade (danger > warning > info > success)
     */
    public List<AlertDTO> generateAlerts(Long userId) {
        log.debug("Gerando alertas para usuário {}", userId);
        List<AlertDTO> alerts = new ArrayList<>();

        // Dados do usuário
        List<Product> products = productRepository.findByUserIdAndActiveTrue(userId);
        List<Transaction> transactions = transactionRepository.findByUserId(userId);

        // 1. Verificar dados suficientes
        if (transactions.size() < 3) {
            alerts.add(AlertDTO.builder()
                    .type("info")
                    .title("Dados Insuficientes")
                    .message("Registre pelo menos 3 meses de transações para análises completas. " +
                             "Quanto mais dados, mais precisa será a previsão.")
                    .icon("fas fa-database")
                    .build());
            return alerts; // Retorna cedo — sem dados para outros alertas
        }

        // 2. Alertas de produto
        alerts.addAll(checkProductMargins(products));

        // 3. Alertas de fluxo de caixa
        alerts.addAll(checkCashFlow(transactions));

        // 4. Alerta de queda de receita
        alerts.addAll(checkRevenueDrop(transactions));

        // 5. Alerta de custos crescentes
        alerts.addAll(checkCostIncrease(transactions));

        // 6. Alerta de saúde financeira positiva
        alerts.addAll(checkFinancialHealth(products, transactions));

        // Ordenar por severidade: danger > warning > info > success
        alerts.sort(Comparator.comparingInt(this::severityOrder));

        log.info("Gerados {} alertas para usuário {}", alerts.size(), userId);
        return alerts;
    }

    /**
     * Verifica margens de produto — alerta se margem < 15% ou negativa.
     */
    private List<AlertDTO> checkProductMargins(List<Product> products) {
        List<AlertDTO> alerts = new ArrayList<>();

        for (Product product : products) {
            if (product.getMargin() == null) continue;

            if (product.getMargin().compareTo(BigDecimal.ZERO) < 0) {
                // Margem negativa — DANGER
                alerts.add(AlertDTO.builder()
                        .type("danger")
                        .title("Margem Negativa")
                        .message(String.format("O produto \"%s\" está com margem de %s%%. " +
                                "Você está perdendo dinheiro a cada venda.",
                                product.getName(), product.getMargin().setScale(1, RoundingMode.HALF_UP)))
                        .icon("fas fa-exclamation-circle")
                        .metric(product.getMargin())
                        .metricLabel("Margem: " + product.getMargin().setScale(1, RoundingMode.HALF_UP) + "%")
                        .build());
            } else if (product.getMargin().compareTo(LOW_MARGIN_THRESHOLD) < 0) {
                // Margem baixa — WARNING
                alerts.add(AlertDTO.builder()
                        .type("warning")
                        .title("Margem Baixa")
                        .message(String.format("O produto \"%s\" tem margem de apenas %s%%. " +
                                "Considere ajustar o preço ou reduzir custos.",
                                product.getName(), product.getMargin().setScale(1, RoundingMode.HALF_UP)))
                        .icon("fas fa-exclamation-triangle")
                        .metric(product.getMargin())
                        .metricLabel("Margem: " + product.getMargin().setScale(1, RoundingMode.HALF_UP) + "%")
                        .build());
            }
        }

        return alerts;
    }

    /**
     * Verifica se o fluxo de caixa dos últimos 3 meses é negativo.
     */
    private List<AlertDTO> checkCashFlow(List<Transaction> transactions) {
        List<AlertDTO> alerts = new ArrayList<>();

        LocalDate threeMonthsAgo = LocalDate.now().minusMonths(3);

        List<Transaction> recentTransactions = transactions.stream()
                .filter(t -> t.getDate().isAfter(threeMonthsAgo))
                .toList();

        if (recentTransactions.isEmpty()) return alerts;

        BigDecimal totalRevenue = recentTransactions.stream()
                .filter(t -> t.getType() == TransactionType.REVENUE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCosts = recentTransactions.stream()
                .filter(t -> t.getType() == TransactionType.FIXED_COST || t.getType() == TransactionType.VARIABLE_COST)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal cashFlow = totalRevenue.subtract(totalCosts);

        if (cashFlow.compareTo(BigDecimal.ZERO) < 0) {
            alerts.add(AlertDTO.builder()
                    .type("danger")
                    .title("Caixa Negativo")
                    .message(String.format("Nos últimos 3 meses, suas despesas superaram suas receitas em R$ %s. " +
                            "Revise seus custos fixos e variáveis urgentemente.",
                            cashFlow.abs().setScale(2, RoundingMode.HALF_UP)
                                    .toString().replace(".", ",")))
                    .icon("fas fa-arrow-down")
                    .metric(cashFlow)
                    .metricLabel("Deficit: R$ " + cashFlow.abs().setScale(2, RoundingMode.HALF_UP)
                            .toString().replace(".", ","))
                    .build());
        }

        return alerts;
    }

    /**
     * Verifica se a receita do último mês caiu abaixo de 80% da média dos 3 anteriores.
     */
    private List<AlertDTO> checkRevenueDrop(List<Transaction> transactions) {
        List<AlertDTO> alerts = new ArrayList<>();

        // Agrupar receitas por mês
        Map<String, BigDecimal> monthlyRevenue = transactions.stream()
                .filter(t -> t.getType() == TransactionType.REVENUE)
                .collect(Collectors.groupingBy(
                        t -> t.getDate().getYear() + "-" + String.format("%02d", t.getDate().getMonthValue()),
                        TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        List<BigDecimal> revenueValues = new ArrayList<>(monthlyRevenue.values());
        if (revenueValues.size() < 4) return alerts; // Precisa de pelo menos 4 meses

        // Média dos 3 meses anteriores ao último
        BigDecimal avgPrevious = BigDecimal.ZERO;
        for (int i = revenueValues.size() - 4; i < revenueValues.size() - 1; i++) {
            avgPrevious = avgPrevious.add(revenueValues.get(i));
        }
        avgPrevious = avgPrevious.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);

        BigDecimal lastMonth = revenueValues.get(revenueValues.size() - 1);

        if (avgPrevious.compareTo(BigDecimal.ZERO) > 0) {
            double ratio = lastMonth.doubleValue() / avgPrevious.doubleValue();

            if (ratio < REVENUE_DROP_THRESHOLD) {
                double dropPercent = (1 - ratio) * 100;
                alerts.add(AlertDTO.builder()
                        .type("warning")
                        .title("Queda de Receita")
                        .message(String.format("A receita do último mês caiu %.0f%% em relação à média dos 3 meses anteriores. " +
                                "Investigue possíveis causas: sazonalidade, perda de clientes, ou concorrência.",
                                dropPercent))
                        .icon("fas fa-chart-line")
                        .metric(BigDecimal.valueOf(dropPercent).setScale(1, RoundingMode.HALF_UP))
                        .metricLabel(String.format("Queda: %.0f%%", dropPercent))
                        .build());
            }
        }

        return alerts;
    }

    /**
     * Verifica se os custos fixos cresceram mais de 10% nos últimos 3 meses.
     */
    private List<AlertDTO> checkCostIncrease(List<Transaction> transactions) {
        List<AlertDTO> alerts = new ArrayList<>();

        // Agrupar custos fixos por mês
        Map<String, BigDecimal> monthlyCosts = transactions.stream()
                .filter(t -> t.getType() == TransactionType.FIXED_COST)
                .collect(Collectors.groupingBy(
                        t -> t.getDate().getYear() + "-" + String.format("%02d", t.getDate().getMonthValue()),
                        TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        List<BigDecimal> costValues = new ArrayList<>(monthlyCosts.values());
        if (costValues.size() < 3) return alerts;

        // Comparar primeiro e último dos 3 últimos meses
        BigDecimal firstMonth = costValues.get(costValues.size() - 3);
        BigDecimal lastMonth = costValues.get(costValues.size() - 1);

        if (firstMonth.compareTo(BigDecimal.ZERO) > 0) {
            double increaseRatio = (lastMonth.doubleValue() - firstMonth.doubleValue()) / firstMonth.doubleValue();

            if (increaseRatio > COST_INCREASE_THRESHOLD) {
                double increasePercent = increaseRatio * 100;
                alerts.add(AlertDTO.builder()
                        .type("warning")
                        .title("Custos Fixos Crescentes")
                        .message(String.format("Seus custos fixos aumentaram %.0f%% nos últimos 3 meses. " +
                                "Renegocie contratos e identifique despesas desnecessárias.",
                                increasePercent))
                        .icon("fas fa-arrow-up")
                        .metric(BigDecimal.valueOf(increasePercent).setScale(1, RoundingMode.HALF_UP))
                        .metricLabel(String.format("Aumento: %.0f%%", increasePercent))
                        .build());
            }
        }

        return alerts;
    }

    /**
     * Gera alerta positivo se o negócio está saudável.
     * Margem > 30% e receita estável ou crescendo.
     */
    private List<AlertDTO> checkFinancialHealth(List<Product> products, List<Transaction> transactions) {
        List<AlertDTO> alerts = new ArrayList<>();

        // Verificar se a maioria dos produtos tem margem saudável
        long healthyProducts = products.stream()
                .filter(p -> p.getMargin() != null && p.getMargin().compareTo(HEALTHY_MARGIN_THRESHOLD) >= 0)
                .count();

        boolean majorityHealthy = products.size() > 0 && healthyProducts > products.size() / 2;

        // Verificar receita crescente (último mês > penúltimo)
        Map<String, BigDecimal> monthlyRevenue = transactions.stream()
                .filter(t -> t.getType() == TransactionType.REVENUE)
                .collect(Collectors.groupingBy(
                        t -> t.getDate().getYear() + "-" + String.format("%02d", t.getDate().getMonthValue()),
                        TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        List<BigDecimal> revenueValues = new ArrayList<>(monthlyRevenue.values());
        boolean revenueGrowing = revenueValues.size() >= 2 &&
                revenueValues.get(revenueValues.size() - 1).compareTo(revenueValues.get(revenueValues.size() - 2)) > 0;

        if (majorityHealthy && revenueGrowing) {
            alerts.add(AlertDTO.builder()
                    .type("success")
                    .title("Saúde Financeira Excelente")
                    .message(String.format("%d de %d produtos com margem acima de 30%% e receita em crescimento. " +
                            "Continue monitorando para manter essa trajetória!",
                            healthyProducts, products.size()))
                    .icon("fas fa-heartbeat")
                    .build());
        } else if (majorityHealthy) {
            alerts.add(AlertDTO.builder()
                    .type("success")
                    .title("Margens Saudáveis")
                    .message(String.format("%d de %d produtos com margem saudável (>30%%). " +
                            "Boa gestão de precificação!",
                            healthyProducts, products.size()))
                    .icon("fas fa-check-circle")
                    .build());
        }

        return alerts;
    }

    /**
     * Ordem de severidade para sorting (menor = mais severo = aparece primeiro).
     */
    private int severityOrder(AlertDTO alert) {
        return switch (alert.getType()) {
            case "danger" -> 0;
            case "warning" -> 1;
            case "info" -> 2;
            case "success" -> 3;
            default -> 4;
        };
    }
}
