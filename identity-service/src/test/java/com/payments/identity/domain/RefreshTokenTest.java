package com.payments.identity.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    @Test
    @DisplayName("Active unexpired refresh token should be valid")
    void activeToken_ShouldBeValid() {
        UUID userId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);
        RefreshToken token = RefreshToken.create(userId, "hashed-token-value", expiresAt);

        assertThat(token.isValid()).isTrue();
        assertThat(token.isRevoked()).isFalse();
        assertThat(token.isExpired()).isFalse();
    }

    @Test
    @DisplayName("Revoking token should make it invalid")
    void revokedToken_ShouldBeInvalid() {
        UUID userId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);
        RefreshToken token = RefreshToken.create(userId, "hashed-token-value", expiresAt);

        token.revoke();

        assertThat(token.isRevoked()).isTrue();
        assertThat(token.isValid()).isFalse();
    }

    @Test
    @DisplayName("Expired token should be invalid")
    void expiredToken_ShouldBeInvalid() {
        UUID userId = UUID.randomUUID();
        Instant expiredTime = Instant.now().minus(1, ChronoUnit.MINUTES);
        RefreshToken token = RefreshToken.create(userId, "hashed-token-value", expiredTime);

        assertThat(token.isExpired()).isTrue();
        assertThat(token.isValid()).isFalse();
    }
}
