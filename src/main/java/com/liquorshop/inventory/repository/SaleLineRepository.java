package com.liquorshop.inventory.repository;

import com.liquorshop.inventory.entity.SaleLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaleLineRepository extends JpaRepository<SaleLineEntity, Long> {

    List<SaleLineEntity> findBySaleId(Long saleId);

    List<SaleLineEntity> findByProductId(Long productId);
}
