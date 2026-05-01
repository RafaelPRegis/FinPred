package com.finpred.core.service;

import com.finpred.core.dto.ImportRowDto;
import com.finpred.core.model.Product;
import com.finpred.core.model.Transaction;
import com.finpred.core.model.TransactionType;
import com.finpred.core.repository.ProductRepository;
import com.finpred.core.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportDataService {

    private final ProductRepository productRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public void saveRow(ImportRowDto row) {
        Long productId = null;

        // Auto-detect and register product
        if (row.getProductName() != null && !row.getProductName().isBlank()) {
            Optional<Product> existingProduct = productRepository.findByUserIdAndNameIgnoreCase(row.getUserId(), row.getProductName().trim());
            
            if (existingProduct.isPresent()) {
                productId = existingProduct.get().getId();
            } else {
                Product newProduct = Product.builder()
                        .userId(row.getUserId())
                        .name(row.getProductName().trim())
                        .salePrice(row.getProductSalePrice() != null ? row.getProductSalePrice() : BigDecimal.ZERO)
                        .costPrice(row.getProductCostPrice() != null ? row.getProductCostPrice() : BigDecimal.ZERO)
                        .estimatedVolume(row.getProductEstimatedVolume() != null ? row.getProductEstimatedVolume().intValue() : 0)
                        .category(row.getCategory() != null ? row.getCategory() : "Importado")
                        .active(true)
                        .build();
                newProduct.calculateMargin();
                Product saved = productRepository.save(newProduct);
                productId = saved.getId();
                log.info("Produto autodetectado e cadastrado: {}", saved.getName());
            }
        }

        // Determine transaction type
        TransactionType type;
        if (row.getTransactionType() != null) {
            try {
                type = TransactionType.valueOf(row.getTransactionType().toUpperCase());
            } catch (IllegalArgumentException e) {
                type = row.getAmount().compareTo(BigDecimal.ZERO) >= 0 ? TransactionType.REVENUE : TransactionType.VARIABLE_COST;
            }
        } else {
            type = row.getAmount().compareTo(BigDecimal.ZERO) >= 0 ? TransactionType.REVENUE : TransactionType.VARIABLE_COST;
        }

        // Register transaction
        Transaction transaction = Transaction.builder()
                .userId(row.getUserId())
                .date(row.getDate())
                .description(row.getDescription() != null ? row.getDescription().trim() : "Importação")
                .amount(row.getAmount().abs())
                .type(type)
                .category(row.getCategory() != null ? row.getCategory() : "Geral")
                .productId(productId)
                .build();

        transactionRepository.save(transaction);
    }
}
