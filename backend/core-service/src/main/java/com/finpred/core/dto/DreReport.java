package com.finpred.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DRE — Demonstração do Resultado do Exercício.
 * 
 * Estrutura contábil completa com todas as linhas de resultado,
 * margens e detalhamento por categoria.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DreReport {

    // === Período ===
    private String periodoInicio;
    private String periodoFim;
    private String periodoLabel;

    // === Linhas do DRE ===

    // 1. Receita Bruta
    private BigDecimal receitaBruta;
    private BigDecimal outrasReceitas;

    // 2. Deduções sobre Receita (impostos sobre faturamento)
    private BigDecimal deducoesSobreReceita;

    // 3. Receita Líquida = Receita Bruta + Outras Receitas - Deduções
    private BigDecimal receitaLiquida;

    // 4. Custo das Mercadorias Vendidas (CMV) / Custo dos Serviços Prestados
    private BigDecimal custoVariavel;

    // 5. Lucro Bruto = Receita Líquida - CMV
    private BigDecimal lucroBruto;
    private BigDecimal margemBruta; // %

    // 6. Despesas Operacionais
    private BigDecimal despesasFixas;
    private BigDecimal depreciacao;
    private BigDecimal despesasFinanceiras;
    private BigDecimal outrasDespesas;
    private BigDecimal totalDespesasOperacionais;

    // 7. Lucro Operacional (EBITDA approximation) = Lucro Bruto - Despesas Operacionais
    private BigDecimal lucroOperacional;
    private BigDecimal margemOperacional; // %

    // 8. Impostos sobre o Lucro (estimativa)
    private BigDecimal impostosSobreLucro;

    // 9. Lucro Líquido = Lucro Operacional - Impostos
    private BigDecimal lucroLiquido;
    private BigDecimal margemLiquida; // %

    // === Detalhamento por categoria ===
    private List<CategoriaDetalhe> detalhamentoReceitas;
    private List<CategoriaDetalhe> detalhamentoCustos;
    private List<CategoriaDetalhe> detalhamentoDespesas;

    // === Dados para gráfico ===
    private List<DreMensal> evolucaoMensal;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoriaDetalhe {
        private String categoria;
        private BigDecimal valor;
        private BigDecimal percentual; // % sobre receita bruta
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DreMensal {
        private String mes;
        private BigDecimal receita;
        private BigDecimal custos;
        private BigDecimal lucro;
    }
}
