package com.liquorshop.inventory.repository;

import com.liquorshop.inventory.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    
    Optional<UserEntity> findByUsername(String username);
    

    Boolean existsByUsername(String username);
    
}
