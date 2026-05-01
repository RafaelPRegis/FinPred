package com.finpred.prediction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyProjection {
    private String monthLabel; // ex: "Jan/2026"
    private BigDecimal revenue;
    private BigDecimal costs;
    private BigDecimal profit;
    private Integer volume;
}
