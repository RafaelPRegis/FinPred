package com.finpred.core.model;

/**
 * Tipos de transação financeira.
 * 
 * Expandido na Fase 6 para suportar DRE completo com linhas contábeis
 * como deduções, depreciação, despesas financeiras e impostos.
 */
public enum TransactionType {
    REVENUE("Receita"),
    FIXED_COST("Custo Fixo"),
    VARIABLE_COST("Custo Variável"),
    TAX("Imposto"),
    DEPRECIATION("Depreciação"),
    FINANCIAL_EXPENSE("Despesa Financeira"),
    OTHER_REVENUE("Outra Receita"),
    OTHER_EXPENSE("Outra Despesa");

    private final String displayName;

    TransactionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
