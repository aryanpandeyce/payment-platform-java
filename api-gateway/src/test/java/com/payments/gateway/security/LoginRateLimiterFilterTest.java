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

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRateLimiterFilterTest {

    private LoginRateLimiterFilter filter;
    private GatewayFilterChain chain;
    private boolean chainCalled;

    @BeforeEach
    void setUp() {
        filter = new LoginRateLimiterFilter();
        chainCalled = false;
        chain = exchange -> {
            chainCalled = true;
            return Mono.empty();
        };
    }

    @Test
    @DisplayName("Requests under threshold should pass through filter")
    void underLimit_ShouldPass() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/auth/login")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 12345))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        assertThat(chainCalled).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    @DisplayName("Non-login endpoints should bypass rate limiter")
    void nonLoginEndpoint_ShouldBypass() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/accounts")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 12345))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        assertThat(chainCalled).isTrue();
    }

    @Test
    @DisplayName("Exceeding limit on /auth/login should return 429 Too Many Requests")
    void exceedingLimit_ShouldReturn429() {
        InetSocketAddress remoteAddress = new InetSocketAddress("192.168.1.100", 50000);

        // Make 15 requests
        for (int i = 0; i < 15; i++) {
            MockServerHttpRequest req = MockServerHttpRequest.post("/auth/login")
                    .remoteAddress(remoteAddress)
                    .build();
            MockServerWebExchange ex = MockServerWebExchange.from(req);
            filter.filter(ex, chain).block();
        }

        // 16th request must be rate limited with 429
        chainCalled = false;
        MockServerHttpRequest blockedReq = MockServerHttpRequest.post("/auth/login")
                .remoteAddress(remoteAddress)
                .build();
        MockServerWebExchange blockedEx = MockServerWebExchange.from(blockedReq);

        filter.filter(blockedEx, chain).block();

        assertThat(chainCalled).isFalse();
        assertThat(blockedEx.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }
}
