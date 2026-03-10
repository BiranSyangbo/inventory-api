package com.liquorshop.inventory.repository;

import com.liquorshop.inventory.entity.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {

    List<CustomerEntity> findAllByOrderByNameAsc();

    Optional<CustomerEntity> findByNameIgnoreCase(String name);
}
