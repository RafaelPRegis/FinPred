package com.finpred.prediction.service;

import com.finpred.prediction.dto.TransactionDTO;
import java.util.List;

public interface CoreServiceClient {
    /**
     * Busca o histórico de transações de um usuário (receitas, custos).
     */
    List<TransactionDTO> getUserTransactions(Long userId, String token);
}
