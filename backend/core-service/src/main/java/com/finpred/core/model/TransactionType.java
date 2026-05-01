package com.finpred.core.model;

/**
 * Tipos de transação financeira.
 */
public enum TransactionType {
    REVENUE("Receita"),
    FIXED_COST("Custo Fixo"),
    VARIABLE_COST("Custo Variável");

    private final String displayName;

    TransactionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
