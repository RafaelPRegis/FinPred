package com.finpred.core.dto;

import com.finpred.core.model.AcquirerRate;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcquirerRateRequest {

    @NotBlank(message = "Nome do adquirente é obrigatório")
    private String name;

    private BigDecimal creditRate;
    private BigDecimal debitRate;
    private BigDecimal pixRate;
    private BigDecimal monthlyFee;
    private Boolean active;
}
