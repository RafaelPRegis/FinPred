package com.finpred.core.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entidade AcquirerRate — taxas de adquirentes de pagamento.
 */
@Entity
@Table(name = "acquirer_rates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcquirerRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    @Column(precision = 5, scale = 2)
    private BigDecimal creditRate;

    @Column(precision = 5, scale = 2)
    private BigDecimal debitRate;

    @Column(precision = 5, scale = 2)
    private BigDecimal pixRate;

    @Column(precision = 12, scale = 2)
    private BigDecimal monthlyFee;

    @Builder.Default
    private Boolean active = true;
}
