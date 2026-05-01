package com.finpred.prediction.service;

import com.finpred.prediction.dto.TransactionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@Profile("dev")
@Slf4j
public class MockCoreServiceClient implements CoreServiceClient {

    @Override
    public List<TransactionDTO> getUserTransactions(Long userId, String token) {
        log.info("Usando MockCoreServiceClient (Perfil DEV) para buscar transações do usuário {}", userId);
        
        List<TransactionDTO> transactions = new ArrayList<>();
        Random random = new Random(userId); // seed fixa por user
        
        // Gerar 12 meses de histórico fake
        LocalDate startDate = LocalDate.now().minusMonths(12);
        
        for (int i = 0; i < 12; i++) {
            LocalDate currentMonth = startDate.plusMonths(i);
            
            // Tendência de crescimento (base 10000 + 500 por mês)
            double baseRevenue = 10000 + (i * 500);
            
            // Sazonalidade (pico no fim do ano)
            if (currentMonth.getMonthValue() == 11 || currentMonth.getMonthValue() == 12) {
                baseRevenue *= 1.4;
            }
            
            // Ruído aleatório +- 10%
            double noise = 0.9 + (0.2 * random.nextDouble());
            BigDecimal finalRevenue = BigDecimal.valueOf(baseRevenue * noise);
            
            transactions.add(TransactionDTO.builder()
                    .id((long) (i * 3 + 1))
                    .userId(userId)
                    .type("REVENUE")
                    .amount(finalRevenue)
                    .date(currentMonth.withDayOfMonth(15))
                    .build());
                    
            // Custos fixos (constantes com pequenos aumentos)
            BigDecimal fixedCost = BigDecimal.valueOf(4000 + (i * 50));
            transactions.add(TransactionDTO.builder()
                    .id((long) (i * 3 + 2))
                    .userId(userId)
                    .type("FIXED_COST")
                    .amount(fixedCost)
                    .date(currentMonth.withDayOfMonth(5))
                    .build());
                    
            // Custos variáveis (proporcionais à receita, aprox 30%)
            BigDecimal variableCost = finalRevenue.multiply(BigDecimal.valueOf(0.3));
            transactions.add(TransactionDTO.builder()
                    .id((long) (i * 3 + 3))
                    .userId(userId)
                    .type("VARIABLE_COST")
                    .amount(variableCost)
                    .date(currentMonth.withDayOfMonth(20))
                    .build());
        }
        
        return transactions;
    }
}
