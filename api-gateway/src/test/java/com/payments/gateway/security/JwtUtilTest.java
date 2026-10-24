package com.payments.gateway.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private final String secret = "supersecretjwtkeythatisatleast256bitslongforhmacsha256signing";
    private final JwtUtil jwtUtil = new JwtUtil(secret, 3600000);

    @Test
    @DisplayName("FR-14: Generates valid token and extracts username")
    void shouldGenerateAndExtractUsername() {
        String token = jwtUtil.generateToken("aryan");
        assertNotNull(token);

        String username = jwtUtil.extractUsername(token);
        assertEquals("aryan", username);
    }

    @Test
    @DisplayName("FR-14: Rejects tampered token")
    void shouldRejectTamperedToken() {
        String token = jwtUtil.generateToken("aryan");
        String tampered = token + "corrupted";

        assertThrows(Exception.class, () -> jwtUtil.validateToken(tampered));
    }
}
