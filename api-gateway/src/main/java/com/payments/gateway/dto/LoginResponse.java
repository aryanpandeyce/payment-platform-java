package com.payments.gateway.dto;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn,
        String username
) {
    public LoginResponse(String token, String username) {
        this(token, "Bearer", 86400, username);
    }
}
