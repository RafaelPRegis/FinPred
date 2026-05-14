package com.finpred.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Projeção de Fluxo de Caixa — histórico real + projeção futura.
 * 
 * Combina dados passados (últimos 6 meses) com projeção via média móvel
 * e sazonalidade, identificando meses de risco (saldo negativo).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashFlowProjection {

    private List<CashFlowMonth> historico;
    private List<CashFlowMonth> projetado;
    private List<String> mesesDeRisco;

    // Resumo
    private BigDecimal saldoAtual;
    private BigDecimal saldoProjetadoFinal;
    private String tendencia; // "CRESCENTE", "ESTÁVEL", "DECRESCENTE"

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CashFlowMonth {
        private String mes;
        private BigDecimal entradas;
        private BigDecimal saidas;
        private BigDecimal saldo;
        private BigDecimal acumulado;
        private boolean risco; // true se acumulado < 0
    }
}
