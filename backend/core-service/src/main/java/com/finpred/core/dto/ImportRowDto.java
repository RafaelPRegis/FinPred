package com.finpred.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportRowDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;
    private LocalDate date;
    private String description;
    private BigDecimal amount;
    private String category;
    private String transactionType; // REVENUE, FIXED_COST, VARIABLE_COST

    // Product info (optional)
    private String productName;
    private BigDecimal productCostPrice;
    private BigDecimal productSalePrice;
    private BigDecimal productEstimatedVolume;
}
