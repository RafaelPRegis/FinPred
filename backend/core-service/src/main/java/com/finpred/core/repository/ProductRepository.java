package com.finpred.core.repository;

import com.finpred.core.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByUserIdAndActiveTrue(Long userId);
    
    Optional<Product> findByUserIdAndNameIgnoreCase(Long userId, String name);
    List<Product> findByUserId(Long userId);
}
