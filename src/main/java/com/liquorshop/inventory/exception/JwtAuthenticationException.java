package com.liquorshop.inventory.exception;

import lombok.Getter;

@Getter
public class JwtAuthenticationException extends RuntimeException {
    
    private final String errorCode;

    public JwtAuthenticationException(String message) {
        super(message);
        this.errorCode = "AUTH_ERROR";
    }

    public JwtAuthenticationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
