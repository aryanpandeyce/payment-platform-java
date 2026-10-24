package com.payments.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// @Component: Stateless JWT token generation and verification utility
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(
            @Value("${jwt.secret:supersecretjwtkeythatisatleast256bitslongforhmacsha256signing}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String username) {
        return generateToken(username, username, "USER");
    }

    public String generateToken(String userId, String username, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId)
                .claim("username", username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    // Token verify karta hai aur signature/expiry check karta hai
    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        Claims claims = validateToken(token);
        String username = claims.get("username", String.class);
        return username != null ? username : claims.getSubject();
    }

    public String extractUserId(String token) {
        return validateToken(token).getSubject();
    }

    public String extractRole(String token) {
        Claims claims = validateToken(token);
        String role = claims.get("role", String.class);
        return role != null ? role : "USER";
    }
}
