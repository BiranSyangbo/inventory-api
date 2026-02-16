package com.liquorshop.inventory.repository;

import com.liquorshop.inventory.model.SaleLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaleLineRepository extends JpaRepository<SaleLine, Long> {
    
    List<SaleLine> findBySaleId(Long saleId);
}
