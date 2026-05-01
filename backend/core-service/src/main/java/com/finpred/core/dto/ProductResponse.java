package com.finpred.core.dto;

import com.finpred.core.model.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private String name;
    private BigDecimal salePrice;
    private BigDecimal costPrice;
    private BigDecimal margin;
    private String category;
    private Integer estimatedVolume;
    private Boolean active;
    private LocalDateTime createdAt;

    public static ProductResponse fromEntity(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .salePrice(p.getSalePrice())
                .costPrice(p.getCostPrice())
                .margin(p.getMargin())
                .category(p.getCategory())
                .estimatedVolume(p.getEstimatedVolume())
                .active(p.getActive())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
