package com.finpred.core.service;

import com.finpred.core.repository.ProductRepository;
import com.finpred.core.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Serviço principal do Core.
 * CRUD de produtos, transações e dashboard serão implementados na Fase 2.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CoreService {

    private final ProductRepository productRepository;
    private final TransactionRepository transactionRepository;

    // TODO: Fase 2 — CRUD Products, CRUD Transactions, Dashboard Summary
}
