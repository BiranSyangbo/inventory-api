package com.liquorshop.inventory.repository;

import com.liquorshop.inventory.entity.SalePaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface SalePaymentRepository extends JpaRepository<SalePaymentEntity, Long> {

    List<SalePaymentEntity> findByCustomerIdOrderByPaymentDateDesc(Long customerId);

    List<SalePaymentEntity> findBySaleIdOrderByPaymentDateDesc(Long saleId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM SalePaymentEntity p WHERE p.sale.id = :saleId")
    BigDecimal sumAmountBySaleId(@Param("saleId") Long saleId);
}
