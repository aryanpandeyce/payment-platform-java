package com.payments.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecurityHeadersGatewayTest {

    @Test
    void shouldAddSecurityHeadersToResponse() {
        WebFilter securityHeaderFilter = (exchange, chain) -> {
            exchange.getResponse().getHeaders().add("X-Content-Type-Options", "nosniff");
            exchange.getResponse().getHeaders().add("X-Frame-Options", "DENY");
            exchange.getResponse().getHeaders().add("X-XSS-Protection", "1; mode=block");
            exchange.getResponse().getHeaders().add("Referrer-Policy", "strict-origin-when-cross-origin");
            return chain.filter(exchange);
        };

        MockServerHttpRequest request = MockServerHttpRequest.get("/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        WebFilterChain chain = (ex) -> Mono.empty();
        securityHeaderFilter.filter(exchange, chain).block();

        assertEquals("nosniff", exchange.getResponse().getHeaders().getFirst("X-Content-Type-Options"));
        assertEquals("DENY", exchange.getResponse().getHeaders().getFirst("X-Frame-Options"));
        assertEquals("1; mode=block", exchange.getResponse().getHeaders().getFirst("X-XSS-Protection"));
        assertEquals("strict-origin-when-cross-origin", exchange.getResponse().getHeaders().getFirst("Referrer-Policy"));
    }
}
