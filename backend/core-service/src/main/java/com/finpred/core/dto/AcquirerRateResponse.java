package com.finpred.core.dto;

import com.finpred.core.model.AcquirerRate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcquirerRateResponse {

    private Long id;
    private String name;
    private BigDecimal creditRate;
    private BigDecimal debitRate;
    private BigDecimal pixRate;
    private BigDecimal monthlyFee;
    private Boolean active;

    public static AcquirerRateResponse fromEntity(AcquirerRate a) {
        return AcquirerRateResponse.builder()
                .id(a.getId())
                .name(a.getName())
                .creditRate(a.getCreditRate())
                .debitRate(a.getDebitRate())
                .pixRate(a.getPixRate())
                .monthlyFee(a.getMonthlyFee())
                .active(a.getActive())
                .build();
    }
}
