package com.finpred.core.service;

import com.finpred.core.dto.ProductRequest;
import com.finpred.core.dto.ProductResponse;
import com.finpred.core.model.Product;
import com.finpred.core.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public List<ProductResponse> findByUser(Long userId) {
        List<Product> products = productRepository.findByUserId(userId);
        if (products.isEmpty()) {
            products = seedInitialProducts(userId);
        }
        return products.stream()
                .map(ProductResponse::fromEntity)
                .toList();
    }

    public List<ProductResponse> findActiveByUser(Long userId) {
        List<Product> products = productRepository.findByUserIdAndActiveTrue(userId);
        if (products.isEmpty() && productRepository.findByUserId(userId).isEmpty()) {
            products = seedInitialProducts(userId);
        }
        return products.stream()
                .map(ProductResponse::fromEntity)
                .toList();
    }

    private List<Product> seedInitialProducts(Long userId) {
        Product p1 = Product.builder()
                .userId(userId).name("Café Expresso").salePrice(new java.math.BigDecimal("5.00")).costPrice(new java.math.BigDecimal("1.50"))
                .category("Bebidas").estimatedVolume(150).active(true).build();
        p1.calculateMargin();
        
        Product p2 = Product.builder()
                .userId(userId).name("Pão de Queijo").salePrice(new java.math.BigDecimal("4.50")).costPrice(new java.math.BigDecimal("2.00"))
                .category("Salgados").estimatedVolume(100).active(true).build();
        p2.calculateMargin();
        
        productRepository.saveAll(List.of(p1, p2));
        log.info("Produtos de exemplo criados para o userId={}", userId);
        return List.of(p1, p2);
    }

    public ProductResponse findById(Long id, Long userId) {
        Product product = productRepository.findById(id)
                .filter(p -> p.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado"));
        return ProductResponse.fromEntity(product);
    }

    @Transactional
    public ProductResponse create(Long userId, ProductRequest request) {
        Product product = Product.builder()
                .userId(userId)
                .name(request.getName())
                .salePrice(request.getSalePrice())
                .costPrice(request.getCostPrice())
                .category(request.getCategory())
                .estimatedVolume(request.getEstimatedVolume())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        product = productRepository.save(product);
        log.info("Produto criado: {} — userId={}", product.getId(), userId);
        return ProductResponse.fromEntity(product);
    }

    @Transactional
    public ProductResponse update(Long id, Long userId, ProductRequest request) {
        Product product = productRepository.findById(id)
                .filter(p -> p.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado"));

        product.setName(request.getName());
        product.setSalePrice(request.getSalePrice());
        product.setCostPrice(request.getCostPrice());
        product.setCategory(request.getCategory());
        if (request.getEstimatedVolume() != null) {
            product.setEstimatedVolume(request.getEstimatedVolume());
        }
        if (request.getActive() != null) product.setActive(request.getActive());

        product = productRepository.save(product);
        log.info("Produto atualizado: {} — userId={}", id, userId);
        return ProductResponse.fromEntity(product);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Product product = productRepository.findById(id)
                .filter(p -> p.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado"));
        productRepository.delete(product);
        log.info("Produto removido: {} — userId={}", id, userId);
    }
}
