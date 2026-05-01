package com.finpred.prediction.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidade FeedbackLog — compara valores previstos vs realizados.
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

    @Column(nullable = false)
    private Long predictionId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal actualValue;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal predictedValue;

    @Column(precision = 7, scale = 4)
    private BigDecimal errorPercentage;

    @Column(precision = 7, scale = 4)
    private BigDecimal mapeScore;

    @Column(precision = 7, scale = 4)
    private BigDecimal correctionFactor;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
