package com.liquorshop.inventory.repository;

import com.liquorshop.inventory.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    // Exclude soft-deleted products
    List<ProductEntity> findAllByDeletedFalseOrderByNameAsc();

    List<ProductEntity> findAllByDeletedFalseAndStatusOrderByNameAsc(String status);

    Optional<ProductEntity> findByBarcodeAndDeletedFalse(String barcode);

    boolean existsByBarcodeAndDeletedFalse(String barcode);
}
