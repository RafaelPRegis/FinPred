package com.finpred.auth.model;

/**
 * Tipos de negócio suportados pelo sistema.
 */
public enum BusinessType {
    INDUSTRIA("Indústria"),
    VAREJO("Varejo"),
    VESTUARIO("Vestuário"),
    ALIMENTACAO("Alimentação"),
    TECNOLOGIA("Tecnologia"),
    SERVICO("Serviço"),
    EDUCACAO("Educação"),
    SAUDE("Saúde");

    private final String displayName;

    BusinessType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
