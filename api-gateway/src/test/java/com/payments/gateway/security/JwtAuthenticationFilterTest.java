package com.payments.gateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    private JwtAuthenticationFilter filter;
    private JwtUtil jwtUtil;
    private final String secret = "supersecretjwtkeythatisatleast256bitslongforhmacsha256signing";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(secret, 3600000);
        filter = new JwtAuthenticationFilter(jwtUtil);
    }

    @Test
    @DisplayName("Public path /auth/login should pass through without token")
    void publicPath_ShouldPassWithoutToken() {
        MockServerHttpRequest req = MockServerHttpRequest.post("/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(req);

        boolean[] chainExecuted = {false};
        GatewayFilterChain chain = ex -> {
            chainExecuted[0] = true;
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(chainExecuted[0]).isTrue();
    }

    @Test
    @DisplayName("Protected path /api/accounts without token should return 401 Unauthorized")
    void protectedPath_WithoutToken_ShouldReturn401() {
        MockServerHttpRequest req = MockServerHttpRequest.get("/api/accounts").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(req);

        boolean[] chainExecuted = {false};
        GatewayFilterChain chain = ex -> {
            chainExecuted[0] = true;
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(chainExecuted[0]).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Protected path /api/accounts with valid token should inject X-User-Id and X-User-Role")
    void protectedPath_WithValidToken_ShouldInjectHeaders() {
        String userId = UUID.randomUUID().toString();
        String token = jwtUtil.generateToken(userId, "aryan.pandey", "ADMIN");

        MockServerHttpRequest req = MockServerHttpRequest.get("/api/accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(req);

        AtomicReference<HttpHeaders> forwardedHeaders = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            forwardedHeaders.set(ex.getRequest().getHeaders());
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(forwardedHeaders.get()).isNotNull();
        assertThat(forwardedHeaders.get().getFirst("X-User-Id")).isEqualTo(userId);
        assertThat(forwardedHeaders.get().getFirst("X-User-Role")).isEqualTo("ADMIN");
        assertThat(forwardedHeaders.get().getFirst("X-User-Name")).isEqualTo("aryan.pandey");
    }
}
