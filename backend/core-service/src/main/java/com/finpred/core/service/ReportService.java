package com.finpred.core.service;

import com.finpred.core.dto.CashFlowProjection;
import com.finpred.core.dto.DreReport;
import com.finpred.core.model.Transaction;
import com.finpred.core.model.TransactionType;
import com.finpred.core.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço de Relatórios Financeiros — Fase 6.
 * 
 * Gera DRE completo e Fluxo de Caixa Projetado a partir das
 * transações reais do usuário.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final TransactionRepository transactionRepository;

    // =========================================================================
    // DRE — Demonstração do Resultado do Exercício
    // =========================================================================

    /**
     * Gera DRE completo para o período especificado.
     * Se sem dados reais, gera DRE demonstrativo.
     */
    public DreReport generateDre(Long userId, LocalDate start, LocalDate end) {
        log.info("Gerando DRE para userId={}, período {} a {}", userId, start, end);

        List<Transaction> transactions = transactionRepository
                .findByUserIdAndDateBetween(userId, start, end);

        if (transactions.isEmpty()) {
            return buildDemoDre(start, end);
        }

        return buildRealDre(transactions, start, end);
    }

    private DreReport buildRealDre(List<Transaction> transactions, LocalDate start, LocalDate end) {
        // Agregar por tipo
        BigDecimal receitaBruta = sumByType(transactions, TransactionType.REVENUE);
        BigDecimal outrasReceitas = sumByType(transactions, TransactionType.OTHER_REVENUE);
        BigDecimal custoVariavel = sumByType(transactions, TransactionType.VARIABLE_COST);
        BigDecimal despesasFixas = sumByType(transactions, TransactionType.FIXED_COST);
        BigDecimal depreciacao = sumByType(transactions, TransactionType.DEPRECIATION);
        BigDecimal despesasFinanceiras = sumByType(transactions, TransactionType.FINANCIAL_EXPENSE);
        BigDecimal outrasDespesas = sumByType(transactions, TransactionType.OTHER_EXPENSE);
        BigDecimal impostos = sumByType(transactions, TransactionType.TAX);

        // Deduções sobre receita (impostos sobre faturamento ~ 8.5% estimado)
        BigDecimal deducoesSobreReceita = impostos.compareTo(BigDecimal.ZERO) > 0
                ? impostos
                : receitaBruta.multiply(new BigDecimal("0.085")).setScale(2, RoundingMode.HALF_UP);

        // Calcular linhas do DRE
        BigDecimal receitaLiquida = receitaBruta.add(outrasReceitas).subtract(deducoesSobreReceita);
        BigDecimal lucroBruto = receitaLiquida.subtract(custoVariavel);
        BigDecimal totalDespesasOp = despesasFixas.add(depreciacao).add(despesasFinanceiras).add(outrasDespesas);
        BigDecimal lucroOperacional = lucroBruto.subtract(totalDespesasOp);

        // Imposto sobre lucro estimado (15% IRPJ + 9% CSLL = 24% sobre lucro positivo)
        BigDecimal impostosSobreLucro = lucroOperacional.compareTo(BigDecimal.ZERO) > 0
                ? lucroOperacional.multiply(new BigDecimal("0.24")).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal lucroLiquido = lucroOperacional.subtract(impostosSobreLucro);

        // Margens
        BigDecimal margemBruta = calcMargin(lucroBruto, receitaBruta);
        BigDecimal margemOperacional = calcMargin(lucroOperacional, receitaBruta);
        BigDecimal margemLiquida = calcMargin(lucroLiquido, receitaBruta);

        // Detalhamento por categoria
        List<DreReport.CategoriaDetalhe> detReceitas = buildCategoryDetail(transactions,
                Set.of(TransactionType.REVENUE, TransactionType.OTHER_REVENUE), receitaBruta);
        List<DreReport.CategoriaDetalhe> detCustos = buildCategoryDetail(transactions,
                Set.of(TransactionType.VARIABLE_COST), receitaBruta);
        List<DreReport.CategoriaDetalhe> detDespesas = buildCategoryDetail(transactions,
                Set.of(TransactionType.FIXED_COST, TransactionType.DEPRECIATION,
                        TransactionType.FINANCIAL_EXPENSE, TransactionType.OTHER_EXPENSE), receitaBruta);

        // Evolução mensal para gráfico
        List<DreReport.DreMensal> evolucao = buildMonthlyEvolution(transactions);

        Locale ptBr = new Locale("pt", "BR");
        String periodoLabel = start.getMonth().getDisplayName(TextStyle.FULL, ptBr) + "/" + start.getYear()
                + " a " + end.getMonth().getDisplayName(TextStyle.FULL, ptBr) + "/" + end.getYear();

        return DreReport.builder()
                .periodoInicio(start.toString())
                .periodoFim(end.toString())
                .periodoLabel(periodoLabel)
                .receitaBruta(receitaBruta)
                .outrasReceitas(outrasReceitas)
                .deducoesSobreReceita(deducoesSobreReceita)
                .receitaLiquida(receitaLiquida)
                .custoVariavel(custoVariavel)
                .lucroBruto(lucroBruto)
                .margemBruta(margemBruta)
                .despesasFixas(despesasFixas)
                .depreciacao(depreciacao)
                .despesasFinanceiras(despesasFinanceiras)
                .outrasDespesas(outrasDespesas)
                .totalDespesasOperacionais(totalDespesasOp)
                .lucroOperacional(lucroOperacional)
                .margemOperacional(margemOperacional)
                .impostosSobreLucro(impostosSobreLucro)
                .lucroLiquido(lucroLiquido)
                .margemLiquida(margemLiquida)
                .detalhamentoReceitas(detReceitas)
                .detalhamentoCustos(detCustos)
                .detalhamentoDespesas(detDespesas)
                .evolucaoMensal(evolucao)
                .build();
    }

    /**
     * Gera DRE demonstrativo para usuários sem dados.
     */
    private DreReport buildDemoDre(LocalDate start, LocalDate end) {
        BigDecimal receitaBruta = new BigDecimal("180000.00");
        BigDecimal outrasReceitas = new BigDecimal("5000.00");
        BigDecimal deducoes = new BigDecimal("15300.00");
        BigDecimal receitaLiquida = new BigDecimal("169700.00");
        BigDecimal cmv = new BigDecimal("72000.00");
        BigDecimal lucroBruto = new BigDecimal("97700.00");
        BigDecimal despFixas = new BigDecimal("35000.00");
        BigDecimal deprec = new BigDecimal("3500.00");
        BigDecimal despFin = new BigDecimal("2800.00");
        BigDecimal outras = new BigDecimal("1500.00");
        BigDecimal totalDesp = new BigDecimal("42800.00");
        BigDecimal lucroOp = new BigDecimal("54900.00");
        BigDecimal impLucro = new BigDecimal("13176.00");
        BigDecimal lucroLiq = new BigDecimal("41724.00");

        return DreReport.builder()
                .periodoInicio(start.toString())
                .periodoFim(end.toString())
                .periodoLabel("Dados demonstrativos — importe transações")
                .receitaBruta(receitaBruta)
                .outrasReceitas(outrasReceitas)
                .deducoesSobreReceita(deducoes)
                .receitaLiquida(receitaLiquida)
                .custoVariavel(cmv)
                .lucroBruto(lucroBruto)
                .margemBruta(new BigDecimal("54.3"))
                .despesasFixas(despFixas)
                .depreciacao(deprec)
                .despesasFinanceiras(despFin)
                .outrasDespesas(outras)
                .totalDespesasOperacionais(totalDesp)
                .lucroOperacional(lucroOp)
                .margemOperacional(new BigDecimal("30.5"))
                .impostosSobreLucro(impLucro)
                .lucroLiquido(lucroLiq)
                .margemLiquida(new BigDecimal("23.2"))
                .detalhamentoReceitas(List.of(
                        new DreReport.CategoriaDetalhe("Vendas", new BigDecimal("150000"), new BigDecimal("83.3")),
                        new DreReport.CategoriaDetalhe("Serviços", new BigDecimal("30000"), new BigDecimal("16.7")),
                        new DreReport.CategoriaDetalhe("Outras", new BigDecimal("5000"), new BigDecimal("2.8"))
                ))
                .detalhamentoCustos(List.of(
                        new DreReport.CategoriaDetalhe("Matéria-prima", new BigDecimal("45000"), new BigDecimal("25.0")),
                        new DreReport.CategoriaDetalhe("Mão de obra direta", new BigDecimal("27000"), new BigDecimal("15.0"))
                ))
                .detalhamentoDespesas(List.of(
                        new DreReport.CategoriaDetalhe("Aluguel", new BigDecimal("12000"), new BigDecimal("6.7")),
                        new DreReport.CategoriaDetalhe("Folha de pagamento", new BigDecimal("18000"), new BigDecimal("10.0")),
                        new DreReport.CategoriaDetalhe("Utilities", new BigDecimal("5000"), new BigDecimal("2.8")),
                        new DreReport.CategoriaDetalhe("Depreciação", new BigDecimal("3500"), new BigDecimal("1.9")),
                        new DreReport.CategoriaDetalhe("Juros/Financeiro", new BigDecimal("2800"), new BigDecimal("1.6")),
                        new DreReport.CategoriaDetalhe("Outros", new BigDecimal("1500"), new BigDecimal("0.8"))
                ))
                .evolucaoMensal(List.of(
                        new DreReport.DreMensal("Jan", new BigDecimal("28000"), new BigDecimal("18000"), new BigDecimal("10000")),
                        new DreReport.DreMensal("Fev", new BigDecimal("26000"), new BigDecimal("17500"), new BigDecimal("8500")),
                        new DreReport.DreMensal("Mar", new BigDecimal("30000"), new BigDecimal("19000"), new BigDecimal("11000")),
                        new DreReport.DreMensal("Abr", new BigDecimal("32000"), new BigDecimal("19500"), new BigDecimal("12500")),
                        new DreReport.DreMensal("Mai", new BigDecimal("31000"), new BigDecimal("19200"), new BigDecimal("11800")),
                        new DreReport.DreMensal("Jun", new BigDecimal("33000"), new BigDecimal("19800"), new BigDecimal("13200"))
                ))
                .build();
    }

    // =========================================================================
    // FLUXO DE CAIXA PROJETADO
    // =========================================================================

    /**
     * Gera projeção de fluxo de caixa: 6 meses históricos + 6 meses projetados.
     */
    public CashFlowProjection generateCashFlowProjection(Long userId) {
        log.info("Gerando Fluxo de Caixa Projetado para userId={}", userId);

        LocalDate now = LocalDate.now();
        LocalDate sixMonthsAgo = now.minusMonths(6).withDayOfMonth(1);

        List<Transaction> transactions = transactionRepository
                .findByUserIdAndDateBetween(userId, sixMonthsAgo, now);

        if (transactions.isEmpty()) {
            return buildDemoCashFlow();
        }

        return buildRealCashFlow(transactions, now);
    }

    private CashFlowProjection buildRealCashFlow(List<Transaction> transactions, LocalDate now) {
        // Agrupar por mês
        Map<YearMonth, List<Transaction>> byMonth = transactions.stream()
                .collect(Collectors.groupingBy(t -> YearMonth.from(t.getDate()), TreeMap::new, Collectors.toList()));

        List<CashFlowProjection.CashFlowMonth> historico = new ArrayList<>();
        BigDecimal acumulado = BigDecimal.ZERO;
        List<BigDecimal> entradasHistory = new ArrayList<>();
        List<BigDecimal> saidasHistory = new ArrayList<>();

        Locale ptBr = new Locale("pt", "BR");

        for (Map.Entry<YearMonth, List<Transaction>> entry : byMonth.entrySet()) {
            YearMonth ym = entry.getKey();
            List<Transaction> monthTx = entry.getValue();

            BigDecimal entradas = monthTx.stream()
                    .filter(t -> t.getType() == TransactionType.REVENUE || t.getType() == TransactionType.OTHER_REVENUE)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal saidas = monthTx.stream()
                    .filter(t -> t.getType() != TransactionType.REVENUE && t.getType() != TransactionType.OTHER_REVENUE)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal saldo = entradas.subtract(saidas);
            acumulado = acumulado.add(saldo);

            entradasHistory.add(entradas);
            saidasHistory.add(saidas);

            String mesLabel = ym.getMonth().getDisplayName(TextStyle.SHORT, ptBr);
            mesLabel = mesLabel.substring(0, 1).toUpperCase() + mesLabel.substring(1) + "/" + ym.getYear();

            historico.add(CashFlowProjection.CashFlowMonth.builder()
                    .mes(mesLabel)
                    .entradas(entradas.setScale(2, RoundingMode.HALF_UP))
                    .saidas(saidas.setScale(2, RoundingMode.HALF_UP))
                    .saldo(saldo.setScale(2, RoundingMode.HALF_UP))
                    .acumulado(acumulado.setScale(2, RoundingMode.HALF_UP))
                    .risco(acumulado.compareTo(BigDecimal.ZERO) < 0)
                    .build());
        }

        // Projetar 6 meses futuros usando média móvel
        BigDecimal mediaEntradas = entradasHistory.isEmpty() ? BigDecimal.ZERO
                : entradasHistory.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(entradasHistory.size()), 2, RoundingMode.HALF_UP);

        BigDecimal mediaSaidas = saidasHistory.isEmpty() ? BigDecimal.ZERO
                : saidasHistory.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(saidasHistory.size()), 2, RoundingMode.HALF_UP);

        // Calcular tendência de crescimento mensal
        BigDecimal growthRate = BigDecimal.ONE;
        if (entradasHistory.size() >= 3) {
            BigDecimal first = entradasHistory.get(0);
            BigDecimal last = entradasHistory.get(entradasHistory.size() - 1);
            if (first.compareTo(BigDecimal.ZERO) > 0) {
                growthRate = last.divide(first, 4, RoundingMode.HALF_UP);
                growthRate = BigDecimal.ONE.add(
                        growthRate.subtract(BigDecimal.ONE)
                                .divide(BigDecimal.valueOf(entradasHistory.size()), 4, RoundingMode.HALF_UP)
                );
            }
        }

        List<CashFlowProjection.CashFlowMonth> projetado = new ArrayList<>();
        List<String> mesesDeRisco = new ArrayList<>();
        YearMonth currentYM = YearMonth.from(now).plusMonths(1);

        for (int i = 0; i < 6; i++) {
            BigDecimal entradasProj = mediaEntradas.multiply(growthRate.pow(i + 1))
                    .setScale(2, RoundingMode.HALF_UP);

            // Sazonalidade básica
            int month = currentYM.getMonthValue();
            if (month == 11 || month == 12) {
                entradasProj = entradasProj.multiply(new BigDecimal("1.3")).setScale(2, RoundingMode.HALF_UP);
            } else if (month == 1 || month == 2) {
                entradasProj = entradasProj.multiply(new BigDecimal("0.85")).setScale(2, RoundingMode.HALF_UP);
            }

            BigDecimal saidasProj = mediaSaidas.multiply(new BigDecimal("1.02").pow(i))
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal saldoProj = entradasProj.subtract(saidasProj);
            acumulado = acumulado.add(saldoProj);
            boolean risco = acumulado.compareTo(BigDecimal.ZERO) < 0;

            String mesLabel = currentYM.getMonth().getDisplayName(TextStyle.SHORT, ptBr);
            mesLabel = mesLabel.substring(0, 1).toUpperCase() + mesLabel.substring(1) + "/" + currentYM.getYear();

            if (risco) mesesDeRisco.add(mesLabel);

            projetado.add(CashFlowProjection.CashFlowMonth.builder()
                    .mes(mesLabel)
                    .entradas(entradasProj)
                    .saidas(saidasProj)
                    .saldo(saldoProj.setScale(2, RoundingMode.HALF_UP))
                    .acumulado(acumulado.setScale(2, RoundingMode.HALF_UP))
                    .risco(risco)
                    .build());

            currentYM = currentYM.plusMonths(1);
        }

        // Determinar tendência
        String tendencia;
        if (growthRate.compareTo(new BigDecimal("1.02")) > 0) {
            tendencia = "CRESCENTE";
        } else if (growthRate.compareTo(new BigDecimal("0.98")) < 0) {
            tendencia = "DECRESCENTE";
        } else {
            tendencia = "ESTÁVEL";
        }

        return CashFlowProjection.builder()
                .historico(historico)
                .projetado(projetado)
                .mesesDeRisco(mesesDeRisco)
                .saldoAtual(historico.isEmpty() ? BigDecimal.ZERO
                        : historico.get(historico.size() - 1).getAcumulado())
                .saldoProjetadoFinal(acumulado.setScale(2, RoundingMode.HALF_UP))
                .tendencia(tendencia)
                .build();
    }

    /**
     * Fluxo de caixa demonstrativo para novos usuários.
     */
    private CashFlowProjection buildDemoCashFlow() {
        List<CashFlowProjection.CashFlowMonth> hist = new ArrayList<>();
        BigDecimal acumulado = BigDecimal.ZERO;

        double[][] demoData = {
                {28000, 18000}, {26000, 17500}, {30000, 19000},
                {32000, 19500}, {31000, 19200}, {33000, 19800}
        };
        String[] mesesHist = {"Jan/2026", "Fev/2026", "Mar/2026", "Abr/2026", "Mai/2026", "Jun/2026"};

        for (int i = 0; i < demoData.length; i++) {
            BigDecimal e = BigDecimal.valueOf(demoData[i][0]);
            BigDecimal s = BigDecimal.valueOf(demoData[i][1]);
            BigDecimal saldo = e.subtract(s);
            acumulado = acumulado.add(saldo);
            hist.add(CashFlowProjection.CashFlowMonth.builder()
                    .mes(mesesHist[i]).entradas(e).saidas(s).saldo(saldo)
                    .acumulado(acumulado).risco(false).build());
        }

        List<CashFlowProjection.CashFlowMonth> proj = new ArrayList<>();
        String[] mesesProj = {"Jul/2026", "Ago/2026", "Set/2026", "Out/2026", "Nov/2026", "Dez/2026"};
        double[][] projData = {
                {34000, 20200}, {35000, 20500}, {36000, 20800},
                {37000, 21000}, {42000, 22000}, {48000, 24000}
        };

        for (int i = 0; i < projData.length; i++) {
            BigDecimal e = BigDecimal.valueOf(projData[i][0]);
            BigDecimal s = BigDecimal.valueOf(projData[i][1]);
            BigDecimal saldo = e.subtract(s);
            acumulado = acumulado.add(saldo);
            proj.add(CashFlowProjection.CashFlowMonth.builder()
                    .mes(mesesProj[i]).entradas(e).saidas(s).saldo(saldo)
                    .acumulado(acumulado).risco(false).build());
        }

        return CashFlowProjection.builder()
                .historico(hist).projetado(proj)
                .mesesDeRisco(List.of()).saldoAtual(hist.get(hist.size() - 1).getAcumulado())
                .saldoProjetadoFinal(acumulado).tendencia("CRESCENTE").build();
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private BigDecimal sumByType(List<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(t -> t.getType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcMargin(BigDecimal value, BigDecimal base) {
        if (base.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return value.divide(base, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP);
    }

    private List<DreReport.CategoriaDetalhe> buildCategoryDetail(
            List<Transaction> transactions, Set<TransactionType> types, BigDecimal receitaBruta) {

        Map<String, BigDecimal> grouped = transactions.stream()
                .filter(t -> types.contains(t.getType()))
                .collect(Collectors.groupingBy(
                        t -> t.getCategory() != null ? t.getCategory() : t.getType().getDisplayName(),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        return grouped.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(e -> DreReport.CategoriaDetalhe.builder()
                        .categoria(e.getKey())
                        .valor(e.getValue().setScale(2, RoundingMode.HALF_UP))
                        .percentual(calcMargin(e.getValue(), receitaBruta))
                        .build())
                .collect(Collectors.toList());
    }

    private List<DreReport.DreMensal> buildMonthlyEvolution(List<Transaction> transactions) {
        Locale ptBr = new Locale("pt", "BR");

        Map<YearMonth, List<Transaction>> byMonth = transactions.stream()
                .collect(Collectors.groupingBy(t -> YearMonth.from(t.getDate()), TreeMap::new, Collectors.toList()));

        return byMonth.entrySet().stream().map(entry -> {
            YearMonth ym = entry.getKey();
            List<Transaction> txs = entry.getValue();

            BigDecimal receita = txs.stream()
                    .filter(t -> t.getType() == TransactionType.REVENUE || t.getType() == TransactionType.OTHER_REVENUE)
                    .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal custos = txs.stream()
                    .filter(t -> t.getType() != TransactionType.REVENUE && t.getType() != TransactionType.OTHER_REVENUE)
                    .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

            String label = ym.getMonth().getDisplayName(TextStyle.SHORT, ptBr);
            label = label.substring(0, 1).toUpperCase() + label.substring(1);

            return DreReport.DreMensal.builder()
                    .mes(label)
                    .receita(receita.setScale(2, RoundingMode.HALF_UP))
                    .custos(custos.setScale(2, RoundingMode.HALF_UP))
                    .lucro(receita.subtract(custos).setScale(2, RoundingMode.HALF_UP))
                    .build();
        }).collect(Collectors.toList());
    }
}
