package com.finpred.core.dto;

import com.finpred.core.model.Transaction;
import com.finpred.core.model.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private Long id;
    private TransactionType type;
    private String description;
    private BigDecimal amount;
    private LocalDate date;
    private Long productId;
    private String category;
    private LocalDateTime createdAt;

    public static TransactionResponse fromEntity(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .type(t.getType())
                .description(t.getDescription())
                .amount(t.getAmount())
                .date(t.getDate())
                .productId(t.getProductId())
                .category(t.getCategory())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
