package com.liquorshop.inventory.repository;

import com.liquorshop.inventory.model.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    
    @Query("SELECT p FROM Purchase p LEFT JOIN FETCH p.purchaseLines pl LEFT JOIN FETCH pl.batch b LEFT JOIN FETCH b.product WHERE p.id = :id")
    Optional<Purchase> findByIdWithBatches(@Param("id") Long id);
}
