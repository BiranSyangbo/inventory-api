package com.liquorshop.inventory.repository;

import com.liquorshop.inventory.entity.PurchaseLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseLineRepository extends JpaRepository<PurchaseLineEntity, Long> {

    List<PurchaseLineEntity> findByPurchaseId(Long purchaseId);

    List<PurchaseLineEntity> findByProductId(Long productId);
}
