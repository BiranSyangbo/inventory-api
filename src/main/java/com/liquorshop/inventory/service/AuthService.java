package com.liquorshop.inventory.service;

import com.liquorshop.inventory.dto.JwtAuthenticationResponse;
import com.liquorshop.inventory.dto.LoginRequest;
import com.liquorshop.inventory.dto.RegisterRequest;
import com.liquorshop.inventory.entity.RefreshTokenEntity;
import com.liquorshop.inventory.entity.UserEntity;
import com.liquorshop.inventory.repository.UserRepository;
import com.liquorshop.inventory.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    @Value("${jwt.expiration:86400000}")
    private Long accessTokenExpirationMs;

    public JwtAuthenticationResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username is already exists!");
        }
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(request.getUsername());
        userEntity.setPassword(passwordEncoder.encode(request.getPassword()));
        userEntity.setEnabled(true);
        userRepository.save(userEntity);

        String accessToken = tokenProvider.generateToken(userEntity.getUsername());
        String refreshToken = refreshTokenService.createRefreshToken(userEntity);

        return buildResponse(accessToken, refreshToken);
    }

    public JwtAuthenticationResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserEntity userEntity = (UserEntity) authentication.getPrincipal();
        String accessToken = tokenProvider.generateToken(authentication);
        String refreshToken = refreshTokenService.createRefreshToken(userEntity);

        return buildResponse(accessToken, refreshToken);
    }

    public JwtAuthenticationResponse refreshTokens(String refreshTokenValue) {
        RefreshTokenEntity refreshTokenEntity = refreshTokenService.findByToken(refreshTokenValue)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshTokenService.isExpired(refreshTokenEntity)) {
            refreshTokenService.revokeByToken(refreshTokenValue);
            throw new RuntimeException("Refresh token has expired");
        }

        UserEntity userEntity = refreshTokenEntity.getUser();
        if (!userEntity.isEnabled()) {
            throw new RuntimeException("User account is disabled");
        }

        // Rotate: revoke current refresh token and issue new one
        refreshTokenService.revokeByToken(refreshTokenValue);
        String newAccessToken = tokenProvider.generateToken(userEntity.getUsername());
        String newRefreshToken = refreshTokenService.createRefreshToken(userEntity);

        return buildResponse(newAccessToken, newRefreshToken);
    }

    public void logout(String refreshTokenValue) {
        if (refreshTokenValue != null && !refreshTokenValue.isBlank()) {
            refreshTokenService.revokeByToken(refreshTokenValue);
        }
    }

    private JwtAuthenticationResponse buildResponse(String accessToken, String refreshToken) {
        JwtAuthenticationResponse response = new JwtAuthenticationResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(accessTokenExpirationMs / 1000);
        response.setRefreshExpiresIn(refreshTokenService.getRefreshTokenDurationMs() / 1000);
        return response;
    }
}
