package com.finpred.core.service;

import com.finpred.core.dto.TaxSimulationRequest;
import com.finpred.core.dto.TaxSimulationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class TaxCalculatorService {

    public TaxSimulationResponse simulate(TaxSimulationRequest request) {
        BigDecimal revenue = request.getRevenue(); // Receita Mensal Simulada
        String taxRegime = request.getTaxRegime() != null ? request.getTaxRegime().toUpperCase() : "SIMPLES_NACIONAL";
        String businessType = request.getBusinessType().toUpperCase();

        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal effectiveRate = BigDecimal.ZERO;
        String rule = "";
        Map<String, BigDecimal> breakdown = new HashMap<>();

        if ("MEI".equals(taxRegime)) {
            if ("COMERCIO".equals(businessType) || "INDUSTRIA".equals(businessType)) {
                taxAmount = new BigDecimal("71.60"); // INSS + ICMS
                rule = "DAS Mensal Fixo (Comércio/Indústria)";
            } else {
                taxAmount = new BigDecimal("75.60"); // INSS + ISS
                rule = "DAS Mensal Fixo (Serviços)";
            }
            if (revenue.compareTo(BigDecimal.ZERO) > 0) {
                effectiveRate = taxAmount.divide(revenue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
            breakdown.put("DAS_MEI", taxAmount);

        } else if ("SIMPLES_NACIONAL".equals(taxRegime)) {
            BigDecimal rbt12 = request.getRbt12() != null && request.getRbt12().compareTo(BigDecimal.ZERO) > 0 
                    ? request.getRbt12() 
                    : revenue.multiply(BigDecimal.valueOf(12)); // Fallback para projeção se não informado
            
            BigDecimal payroll = request.getPayrollAmount() != null ? request.getPayrollAmount() : BigDecimal.ZERO;
            
            SimplesBracket bracket;
            if ("COMERCIO".equals(businessType)) {
                bracket = getAnexoI(rbt12);
                rule = "Simples Nacional - Anexo I (Comércio)";
            } else if ("INDUSTRIA".equals(businessType)) {
                bracket = getAnexoII(rbt12);
                rule = "Simples Nacional - Anexo II (Indústria)";
            } else {
                // Serviço: Cálculo Fator R
                BigDecimal fatorR = rbt12.compareTo(BigDecimal.ZERO) > 0 
                        ? payroll.divide(rbt12, 4, RoundingMode.HALF_UP) 
                        : BigDecimal.ZERO;
                
                if (fatorR.compareTo(new BigDecimal("0.28")) >= 0) {
                    bracket = getAnexoIII(rbt12);
                    rule = "Simples Nacional - Anexo III (Serviços - Fator R >= 28%)";
                } else {
                    bracket = getAnexoV(rbt12);
                    rule = "Simples Nacional - Anexo V (Serviços - Fator R < 28%)";
                }
            }

            // Fórmula: ((RBT12 * Aliquota Nominal) - Parcela a Deduzir) / RBT12
            BigDecimal nominalRate = bracket.rate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            if (rbt12.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal effectiveRateDec = (rbt12.multiply(nominalRate).subtract(bracket.deduction))
                        .divide(rbt12, 4, RoundingMode.HALF_UP);
                if (effectiveRateDec.compareTo(BigDecimal.ZERO) < 0) effectiveRateDec = BigDecimal.ZERO;
                effectiveRate = effectiveRateDec.multiply(BigDecimal.valueOf(100));
            } else {
                effectiveRate = bracket.rate; // Se não tem RBT12, usa alíquota base
            }

            taxAmount = revenue.multiply(effectiveRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            breakdown.put("DAS_SIMPLES", taxAmount);

        } else if ("LUCRO_PRESUMIDO".equals(taxRegime)) {
            boolean isService = "SERVICO".equals(businessType);
            
            // Presunções
            BigDecimal presuncaoIRPJ = isService ? new BigDecimal("0.32") : new BigDecimal("0.08");
            BigDecimal presuncaoCSLL = isService ? new BigDecimal("0.32") : new BigDecimal("0.12");

            BigDecimal baseIRPJ = revenue.multiply(presuncaoIRPJ);
            BigDecimal baseCSLL = revenue.multiply(presuncaoCSLL);

            // Alíquotas base
            BigDecimal pis = revenue.multiply(new BigDecimal("0.0065")); // 0.65%
            BigDecimal cofins = revenue.multiply(new BigDecimal("0.03")); // 3%
            BigDecimal csll = baseCSLL.multiply(new BigDecimal("0.09")); // 9% da base
            
            // IRPJ = 15% + 10% sobre o que exceder 20k no mês
            BigDecimal irpjBaseTax = baseIRPJ.multiply(new BigDecimal("0.15"));
            BigDecimal irpjAdicional = BigDecimal.ZERO;
            if (baseIRPJ.compareTo(new BigDecimal("20000")) > 0) {
                irpjAdicional = baseIRPJ.subtract(new BigDecimal("20000")).multiply(new BigDecimal("0.10"));
            }
            BigDecimal irpj = irpjBaseTax.add(irpjAdicional);
            
            // ISS ou ICMS (Simplificado)
            BigDecimal issIcms;
            if (isService) {
                issIcms = revenue.multiply(new BigDecimal("0.05")); // ISS médio 5%
                breakdown.put("ISS", issIcms.setScale(2, RoundingMode.HALF_UP));
            } else {
                issIcms = revenue.multiply(new BigDecimal("0.18")); // ICMS médio 18%
                breakdown.put("ICMS", issIcms.setScale(2, RoundingMode.HALF_UP));
            }

            taxAmount = pis.add(cofins).add(csll).add(irpj).add(issIcms);
            
            if (revenue.compareTo(BigDecimal.ZERO) > 0) {
                effectiveRate = taxAmount.divide(revenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            }

            breakdown.put("PIS", pis.setScale(2, RoundingMode.HALF_UP));
            breakdown.put("COFINS", cofins.setScale(2, RoundingMode.HALF_UP));
            breakdown.put("CSLL", csll.setScale(2, RoundingMode.HALF_UP));
            breakdown.put("IRPJ", irpj.setScale(2, RoundingMode.HALF_UP));

            rule = "Lucro Presumido (" + (isService ? "Serviços" : "Comércio/Indústria") + ")";
        }

        return TaxSimulationResponse.builder()
                .monthlyRevenue(revenue)
                .estimatedTax(taxAmount.setScale(2, RoundingMode.HALF_UP))
                .effectiveRate(effectiveRate.setScale(2, RoundingMode.HALF_UP))
                .appliedRule(rule)
                .taxBreakdown(breakdown)
                .build();
    }

    private static class SimplesBracket {
        BigDecimal rate;
        BigDecimal deduction;
        SimplesBracket(String rate, String deduction) {
            this.rate = new BigDecimal(rate);
            this.deduction = new BigDecimal(deduction);
        }
    }

    private SimplesBracket getAnexoI(BigDecimal rbt12) {
        if (rbt12.compareTo(new BigDecimal("180000")) <= 0) return new SimplesBracket("4.00", "0.00");
        if (rbt12.compareTo(new BigDecimal("360000")) <= 0) return new SimplesBracket("7.30", "5940.00");
        if (rbt12.compareTo(new BigDecimal("720000")) <= 0) return new SimplesBracket("9.50", "13860.00");
        if (rbt12.compareTo(new BigDecimal("1800000")) <= 0) return new SimplesBracket("10.70", "22500.00");
        if (rbt12.compareTo(new BigDecimal("3600000")) <= 0) return new SimplesBracket("14.30", "87300.00");
        return new SimplesBracket("19.00", "378000.00");
    }

    private SimplesBracket getAnexoII(BigDecimal rbt12) {
        if (rbt12.compareTo(new BigDecimal("180000")) <= 0) return new SimplesBracket("4.50", "0.00");
        if (rbt12.compareTo(new BigDecimal("360000")) <= 0) return new SimplesBracket("7.80", "5940.00");
        if (rbt12.compareTo(new BigDecimal("720000")) <= 0) return new SimplesBracket("10.00", "13860.00");
        if (rbt12.compareTo(new BigDecimal("1800000")) <= 0) return new SimplesBracket("11.20", "22500.00");
        if (rbt12.compareTo(new BigDecimal("3600000")) <= 0) return new SimplesBracket("14.70", "85500.00");
        return new SimplesBracket("30.00", "720000.00");
    }

    private SimplesBracket getAnexoIII(BigDecimal rbt12) {
        if (rbt12.compareTo(new BigDecimal("180000")) <= 0) return new SimplesBracket("6.00", "0.00");
        if (rbt12.compareTo(new BigDecimal("360000")) <= 0) return new SimplesBracket("11.20", "9360.00");
        if (rbt12.compareTo(new BigDecimal("720000")) <= 0) return new SimplesBracket("13.50", "17640.00");
        if (rbt12.compareTo(new BigDecimal("1800000")) <= 0) return new SimplesBracket("16.00", "35640.00");
        if (rbt12.compareTo(new BigDecimal("3600000")) <= 0) return new SimplesBracket("21.00", "125640.00");
        return new SimplesBracket("33.00", "648000.00");
    }

    private SimplesBracket getAnexoV(BigDecimal rbt12) {
        if (rbt12.compareTo(new BigDecimal("180000")) <= 0) return new SimplesBracket("15.50", "0.00");
        if (rbt12.compareTo(new BigDecimal("360000")) <= 0) return new SimplesBracket("18.00", "4500.00");
        if (rbt12.compareTo(new BigDecimal("720000")) <= 0) return new SimplesBracket("19.50", "9900.00");
        if (rbt12.compareTo(new BigDecimal("1800000")) <= 0) return new SimplesBracket("20.50", "17100.00");
        if (rbt12.compareTo(new BigDecimal("3600000")) <= 0) return new SimplesBracket("23.00", "62100.00");
        return new SimplesBracket("30.50", "540000.00");
    }
}
