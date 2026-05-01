package com.finpred.core.service;

import com.finpred.core.dto.TransactionRequest;
import com.finpred.core.dto.TransactionResponse;
import com.finpred.core.model.Transaction;
import com.finpred.core.model.TransactionType;
import com.finpred.core.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public List<TransactionResponse> findByUser(Long userId) {
        return transactionRepository.findByUserId(userId).stream()
                .map(TransactionResponse::fromEntity)
                .toList();
    }

    public List<TransactionResponse> findByUserAndType(Long userId, TransactionType type) {
        return transactionRepository.findByUserIdAndType(userId, type).stream()
                .map(TransactionResponse::fromEntity)
                .toList();
    }

    public List<TransactionResponse> findByUserAndDateRange(Long userId, LocalDate start, LocalDate end) {
        return transactionRepository.findByUserIdAndDateBetween(userId, start, end).stream()
                .map(TransactionResponse::fromEntity)
                .toList();
    }

    @Transactional
    public TransactionResponse create(Long userId, TransactionRequest request) {
        Transaction transaction = Transaction.builder()
                .userId(userId)
                .type(request.getType())
                .description(request.getDescription())
                .amount(request.getAmount())
                .date(request.getDate())
                .productId(request.getProductId())
                .category(request.getCategory())
                .build();

        transaction = transactionRepository.save(transaction);
        log.info("Transação criada: {} — userId={}, type={}", transaction.getId(), userId, request.getType());
        return TransactionResponse.fromEntity(transaction);
    }

    @Transactional
    public TransactionResponse update(Long id, Long userId, TransactionRequest request) {
        Transaction transaction = transactionRepository.findById(id)
                .filter(t -> t.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Transação não encontrada"));

        transaction.setType(request.getType());
        transaction.setDescription(request.getDescription());
        transaction.setAmount(request.getAmount());
        transaction.setDate(request.getDate());
        transaction.setProductId(request.getProductId());
        transaction.setCategory(request.getCategory());

        transaction = transactionRepository.save(transaction);
        log.info("Transação atualizada: {} — userId={}", id, userId);
        return TransactionResponse.fromEntity(transaction);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Transaction transaction = transactionRepository.findById(id)
                .filter(t -> t.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Transação não encontrada"));
        transactionRepository.delete(transaction);
        log.info("Transação removida: {} — userId={}", id, userId);
    }
}
