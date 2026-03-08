package com.liquorshop.inventory.repository;

import com.liquorshop.inventory.entity.RefreshTokenEntity;
import com.liquorshop.inventory.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByToken(String token);

    @Modifying
    void deleteByUser(UserEntity userEntity);

    @Modifying
    void deleteByExpiryAtBefore(Instant instant);
}
