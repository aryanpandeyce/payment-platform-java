package com.payments.identity.dto;

public record RefreshTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken
) {
    public static RefreshTokenResponse of(String accessToken, long expiresIn, String refreshToken) {
        return new RefreshTokenResponse(accessToken, "Bearer", expiresIn, refreshToken);
    }
}
