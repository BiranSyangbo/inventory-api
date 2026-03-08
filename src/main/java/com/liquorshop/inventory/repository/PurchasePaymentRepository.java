package com.liquorshop.inventory.repository;

import com.liquorshop.inventory.entity.PurchasePaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PurchasePaymentRepository extends JpaRepository<PurchasePaymentEntity, Long> {

    List<PurchasePaymentEntity> findByPurchaseIdOrderByPaymentDateDesc(Long purchaseId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PurchasePaymentEntity p WHERE p.purchase.id = :purchaseId")
    BigDecimal sumAmountByPurchaseId(@Param("purchaseId") Long purchaseId);
}
