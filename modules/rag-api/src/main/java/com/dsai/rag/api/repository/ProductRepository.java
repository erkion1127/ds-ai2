package com.dsai.rag.api.repository;

import com.dsai.rag.api.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    Optional<Product> findByProductCode(String productCode);
    
    Optional<Product> findByProductName(String productName);
    
    List<Product> findByProductType(String productType);
    
    List<Product> findByStatus(String status);
    
    @Query("SELECT DISTINCT p.productType FROM Product p WHERE p.productType IS NOT NULL")
    List<String> findAllProductTypes();
}