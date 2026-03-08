package com.liquorshop.inventory.repository;

import com.liquorshop.inventory.entity.SaleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SaleRepository extends JpaRepository<SaleEntity, Long> {

    @Query("""
        SELECT s FROM SaleEntity s
        LEFT JOIN FETCH s.customer
        LEFT JOIN FETCH s.saleLines sl
        LEFT JOIN FETCH sl.batch
        LEFT JOIN FETCH sl.product
        WHERE s.id = :id
        """)
    Optional<SaleEntity> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT s FROM SaleEntity s LEFT JOIN FETCH s.customer ORDER BY s.saleDate DESC")
    List<SaleEntity> findAllWithCustomer();

    List<SaleEntity> findByCustomerIdOrderBySaleDateDesc(Long customerId);

    // Next invoice sequence number for the given year prefix
    @Query("SELECT COUNT(s) FROM SaleEntity s WHERE s.invoiceNumber LIKE :prefix%")
    long countByInvoiceNumberPrefix(@Param("prefix") String prefix);
}
