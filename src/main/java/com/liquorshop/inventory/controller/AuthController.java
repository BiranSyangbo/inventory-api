package com.liquorshop.inventory.controller;

import com.liquorshop.inventory.dto.JwtAuthenticationResponse;
import com.liquorshop.inventory.dto.LoginRequest;
import com.liquorshop.inventory.dto.RefreshTokenRequest;
import com.liquorshop.inventory.dto.RegisterRequest;
import com.liquorshop.inventory.dto.TokenValidationResponse;
import com.liquorshop.inventory.security.JwtTokenProvider;
import com.liquorshop.inventory.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider tokenProvider;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            JwtAuthenticationResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            JwtAuthenticationResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            JwtAuthenticationResponse response = authService.refreshTokens(request.getRefreshToken());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Invalid or expired refresh token"));
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            TokenValidationResponse response = new TokenValidationResponse();
            response.setValid(false);
            response.setExpired(false);
            response.setMessage("No token provided");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        String token = bearerToken.substring(7);
        
        try {
            // Check if token is expired
            Boolean expired = tokenProvider.isTokenExpired(token);
            
            if (expired) {
                TokenValidationResponse response = new TokenValidationResponse();
                response.setValid(false);
                response.setExpired(true);
                response.setMessage("Token has expired");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // Check if token format is valid
            if (!tokenProvider.isTokenValidFormat(token)) {
                TokenValidationResponse response = new TokenValidationResponse();
                response.setValid(false);
                response.setExpired(false);
                response.setMessage("Invalid token format");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // Token is valid
            String username = tokenProvider.getUsernameFromToken(token);
            Date expirationDate = tokenProvider.getTokenExpirationDate(token);
            
            TokenValidationResponse response = new TokenValidationResponse();
            response.setValid(true);
            response.setExpired(false);
            response.setUsername(username);
            response.setExpirationDate(expirationDate);
            response.setMessage("Token is valid");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            TokenValidationResponse response = new TokenValidationResponse();
            response.setValid(false);
            response.setExpired(false);
            response.setMessage("Token validation failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody(required = false) java.util.Map<String, String> body) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String refreshToken = body != null ? body.get("refreshToken") : null;
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        if (authentication != null) {
            SecurityContextHolder.clearContext();
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        return ResponseEntity.ok(Map.of(
            "username", authentication.getName(),
            "authorities", authentication.getAuthorities()
        ));
    }
}
