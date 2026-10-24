package com.payments.gateway.security;

import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

// @Component: Spring Cloud Gateway GlobalFilter
// Downstream microservices par jaane se pehle JWT token validate karta hai aur X-User-Id, X-User-Role inject karta hai
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Public endpoints skip karo (Auth, Actuator, Swagger UI)
        if (path.startsWith("/auth/") || path.startsWith("/actuator/") || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")) {
            return chain.filter(exchange);
        }

        // Authorization header check
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Unauthorized request to '{}': Missing or invalid Authorization header", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7).trim();

        try {
            // Verify JWT signature and expiration
            Claims claims = jwtUtil.validateToken(token);
            String userId = claims.getSubject();
            String username = claims.get("username", String.class) != null ? claims.get("username", String.class) : userId;
            String role = claims.get("role", String.class) != null ? claims.get("role", String.class) : "USER";

            // Invariant FR-14 & FR-21: Forward verified X-User-Id, X-User-Role, and X-User-Name downstream
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Role", role)
                    .header("X-User-Name", username)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            log.warn("JWT validation failed for path '{}': {}", path, e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1; // High precedence filter
    }
}
