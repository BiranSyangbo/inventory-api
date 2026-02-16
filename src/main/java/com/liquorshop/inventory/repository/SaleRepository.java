package com.liquorshop.inventory.repository;

import com.liquorshop.inventory.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {
    
    @Query("SELECT s FROM Sale s LEFT JOIN FETCH s.saleLines sl LEFT JOIN FETCH sl.batch WHERE s.id = :id")
    Optional<Sale> findByIdWithLines(@Param("id") Long id);
}
