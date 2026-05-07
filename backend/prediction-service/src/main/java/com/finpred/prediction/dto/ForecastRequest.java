package com.finpred.prediction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para previsão baseada em ML usando o histórico real do usuário.
 * Diferente do SimulationRequest (que é paramétrico), este usa dados
 * de transações reais para prever o futuro.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForecastRequest {

    /** ID do produto específico para prever (null = todos) */
    private Long productId;

    /** Horizonte de previsão em meses (padrão: 12) */
    @Builder.Default
    private int horizon = 12;

    /** Se true, força recalculo mesmo que exista predição recente */
    @Builder.Default
    private boolean forceRefresh = false;
}
