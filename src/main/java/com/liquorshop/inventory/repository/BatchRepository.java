package com.liquorshop.inventory.repository;

import com.liquorshop.inventory.entity.BatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BatchRepository extends JpaRepository<BatchEntity, Long> {

    List<BatchEntity> findByProductIdOrderByExpiryDateAscCreatedAtAsc(Long productId);

    // All batches with remaining stock, ordered expiry-first for sale allocation
    @Query("""
        SELECT b FROM BatchEntity b
        WHERE b.product.id = :productId AND b.currentQuantity > 0
        ORDER BY
            CASE WHEN b.expiryDate IS NULL THEN 1 ELSE 0 END ASC,
            b.expiryDate ASC NULLS LAST,
            b.createdAt ASC
        """)
    List<BatchEntity> findAvailableByProductId(@Param("productId") Long productId);

    // Batches expiring on or before the given date with stock remaining
    @Query("""
        SELECT b FROM BatchEntity b
        WHERE b.expiryDate IS NOT NULL
          AND b.expiryDate <= :cutoffDate
          AND b.currentQuantity > 0
        ORDER BY b.expiryDate ASC
        """)
    List<BatchEntity> findExpiringWithStock(@Param("cutoffDate") LocalDate cutoffDate);

    // Total current stock for a product across all batches
    @Query("SELECT COALESCE(SUM(b.currentQuantity), 0) FROM BatchEntity b WHERE b.product.id = :productId")
    Integer sumCurrentQuantityByProductId(@Param("productId") Long productId);
}
