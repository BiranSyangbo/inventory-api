package com.liquorshop.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtAuthenticationResponse {

    private String accessToken;
    private String refreshToken;
    private Long expiresIn;       // access token TTL in seconds
    private Long refreshExpiresIn; // refresh token TTL in seconds
}
