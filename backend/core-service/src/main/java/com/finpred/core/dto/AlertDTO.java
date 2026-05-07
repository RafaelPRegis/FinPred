package com.finpred.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de alerta inteligente — gerado pela análise financeira do AlertService.
 * 
 * Tipos de alerta:
 * - success: Saúde financeira positiva
 * - warning: Atenção necessária
 * - danger:  Situação crítica
 * - info:    Informativo / sugestão
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertDTO {

    /** Tipo de alerta: success, warning, danger, info */
    private String type;

    /** Título curto do alerta */
    private String title;

    /** Mensagem descritiva com contexto */
    private String message;

    /** Classe do ícone Font Awesome */
    private String icon;

    /** Valor numérico associado (ex: margem em %, receita em R$) — pode ser null */
    private BigDecimal metric;

    /** Label do valor métrico (ex: "Margem: 8.2%") */
    private String metricLabel;

    /** Timestamp de geração do alerta */
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
