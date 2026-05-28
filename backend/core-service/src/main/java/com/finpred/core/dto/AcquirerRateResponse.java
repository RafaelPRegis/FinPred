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
    private BigDecimal creditRateInstallment2to6;
    private BigDecimal creditRateInstallment7to12;
    private BigDecimal debitRate;
    private BigDecimal pixRate;
    private BigDecimal monthlyFee;
    private Boolean active;

    public static AcquirerRateResponse fromEntity(AcquirerRate a) {
        return AcquirerRateResponse.builder()
                .id(a.getId())
                .name(a.getName())
                .creditRate(a.getCreditRate())
                .creditRateInstallment2to6(a.getCreditRateInstallment2to6())
                .creditRateInstallment7to12(a.getCreditRateInstallment7to12())
                .debitRate(a.getDebitRate())
                .pixRate(a.getPixRate())
                .monthlyFee(a.getMonthlyFee())
                .active(a.getActive())
                .build();
    }
}
