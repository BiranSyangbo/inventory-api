package com.liquorshop.inventory.repository;

import com.liquorshop.inventory.entity.CustomerPriceTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerPriceTemplateRepository extends JpaRepository<CustomerPriceTemplateEntity, Long> {

    @Query("SELECT t FROM CustomerPriceTemplateEntity t JOIN FETCH t.product WHERE t.customer.id = :customerId")
    List<CustomerPriceTemplateEntity> findByCustomerIdWithProduct(@Param("customerId") Long customerId);

    Optional<CustomerPriceTemplateEntity> findByCustomerIdAndProductId(Long customerId, Long productId);

    void deleteByCustomerIdAndProductId(Long customerId, Long productId);
}
