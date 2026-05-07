package com.finpred.prediction.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidade FeedbackLog — compara valores previstos vs realizados.
 * 
 * Cada registro representa a comparação de uma previsão passada com
 * o valor real observado pelo usuário. Isso alimenta o cálculo do MAPE
 * e o fator de correção automático do modelo.
 */
@Entity
@Table(name = "feedback_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID do usuário que enviou o feedback */
    @Column(nullable = false)
    private Long userId;

    /** Referência à predição sendo avaliada */
    @Column(nullable = false)
    private Long predictionId;

    /** Mês/ano referente ao feedback (ex: 2026-01-01 para Janeiro/2026) */
    @Column(nullable = false)
    private LocalDate month;

    /** Valor real observado no período */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal actualValue;

    /** Valor que havia sido previsto pelo modelo */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal predictedValue;

    /** Erro percentual absoluto: |actual - predicted| / |actual| * 100 */
    @Column(precision = 7, scale = 4)
    private BigDecimal errorPercentage;

    /** MAPE acumulado do usuário no momento do feedback */
    @Column(precision = 7, scale = 4)
    private BigDecimal mapeScore;

    /** Fator de correção calculado no momento do feedback */
    @Column(precision = 7, scale = 4)
    private BigDecimal correctionFactor;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
