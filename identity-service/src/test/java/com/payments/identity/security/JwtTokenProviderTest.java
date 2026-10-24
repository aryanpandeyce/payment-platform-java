package com.payments.identity.security;

import com.payments.identity.domain.User;
import com.payments.identity.domain.UserRole;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String secret = "supersecretjwtkeythatisatleast256bitslongforhmacsha256signing";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(secret, 900); // 15 minutes
    }

    @Test
    @DisplayName("Generated JWT token should contain userId as sub, username, and role claims")
    void generateToken_ShouldContainExpectedClaims() {
        UUID userId = UUID.randomUUID();
        User user = User.create(
                "aryan.pandey",
                "aryan.pandey@apexpay.demo",
                "Aryan Pandey",
                "$2a$12$sampleHash",
                UserRole.ADMIN
        );

        String token = jwtTokenProvider.generateAccessToken(user);

        assertThat(token).isNotBlank();
        Claims claims = jwtTokenProvider.parseClaims(token);
        assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
        assertThat(claims.get("username", String.class)).isEqualTo("aryan.pandey");
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Valid token should pass validation")
    void validateToken_ShouldReturnTrueForValidToken() {
        User user = User.create(
                "rahul.sharma",
                "rahul@apexpay.demo",
                "Rahul Sharma",
                "$2a$12$sampleHash",
                UserRole.USER
        );

        String token = jwtTokenProvider.generateAccessToken(user);

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }
}
