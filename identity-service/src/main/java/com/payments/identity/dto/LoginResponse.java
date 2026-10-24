package com.payments.identity.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken,
        UserResponse user
) {
    public static LoginResponse of(String accessToken, long expiresIn, String refreshToken, UserResponse user) {
        return new LoginResponse(accessToken, "Bearer", expiresIn, refreshToken, user);
    }
}
