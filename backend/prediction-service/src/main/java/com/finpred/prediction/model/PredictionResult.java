package com.finpred.prediction.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidade PredictionResult — armazena o resultado de uma predição.
 */
@Entity
@Table(name = "prediction_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate month;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal predictedRevenue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Scenario scenario;

    @Column(precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    /** Parâmetros usados na predição (JSON) */
    @Column(columnDefinition = "TEXT")
    private String parameters;

    private Long productId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
