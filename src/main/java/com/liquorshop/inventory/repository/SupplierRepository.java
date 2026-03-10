package com.liquorshop.inventory.repository;

import com.liquorshop.inventory.entity.SupplierEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<SupplierEntity, Long> {

    List<SupplierEntity> findAllByOrderByNameAsc();

    List<SupplierEntity> findAllByStatusOrderByNameAsc(String status);

    Optional<SupplierEntity> findByNameIgnoreCase(String name);
}
