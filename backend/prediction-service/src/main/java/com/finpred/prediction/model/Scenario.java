package com.finpred.prediction.model;

/**
 * Cenários de predição.
 */
public enum Scenario {
    PESSIMIST("Pessimista", 0.85),
    NEUTRAL("Neutro", 1.00),
    OPTIMIST("Otimista", 1.15);

    private final String displayName;
    private final double multiplier;

    Scenario(String displayName, double multiplier) {
        this.displayName = displayName;
        this.multiplier = multiplier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getMultiplier() {
        return multiplier;
    }
}
