package com.liquorshop.inventory.service;

import com.liquorshop.inventory.entity.RefreshTokenEntity;
import com.liquorshop.inventory.entity.UserEntity;
import com.liquorshop.inventory.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration:604800000}") // default 7 days in ms
    private Long refreshTokenDurationMs;

    @Transactional
    public String createRefreshToken(UserEntity userEntity) {
        RefreshTokenEntity refreshTokenEntity = new RefreshTokenEntity();
        refreshTokenEntity.setUser(userEntity);
        refreshTokenEntity.setToken(UUID.randomUUID().toString());
        refreshTokenEntity.setExpiryAt(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshTokenEntity = refreshTokenRepository.save(refreshTokenEntity);
        return refreshTokenEntity.getToken();
    }

    @Transactional
    public Optional<RefreshTokenEntity> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public boolean isExpired(RefreshTokenEntity token) {
        return token.getExpiryAt().isBefore(Instant.now());
    }

    @Transactional
    public void revokeByToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshTokenRepository::delete);
    }

    @Transactional
    public void revokeByUser(UserEntity userEntity) {
        refreshTokenRepository.deleteByUser(userEntity);
    }

    public long getRefreshTokenDurationMs() {
        return refreshTokenDurationMs;
    }
}
