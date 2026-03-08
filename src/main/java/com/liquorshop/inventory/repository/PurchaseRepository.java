package com.liquorshop.inventory.repository;

import com.liquorshop.inventory.entity.PurchaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseRepository extends JpaRepository<PurchaseEntity, Long> {

    @Query("""
        SELECT p FROM PurchaseEntity p
        JOIN FETCH p.supplier
        LEFT JOIN FETCH p.purchaseLines pl
        LEFT JOIN FETCH pl.batch
        LEFT JOIN FETCH pl.product
        WHERE p.id = :id
        """)
    Optional<PurchaseEntity> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT p FROM PurchaseEntity p JOIN FETCH p.supplier ORDER BY p.purchaseDate DESC")
    List<PurchaseEntity> findAllWithSupplier();

    boolean existsByVatBillNumber(String vatBillNumber);
}
