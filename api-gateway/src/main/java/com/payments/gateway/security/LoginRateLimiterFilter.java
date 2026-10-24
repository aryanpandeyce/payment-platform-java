package com.payments.gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// In-memory rate limiting filter for /auth/login (Max 15 requests per minute per client IP)
// Trade-off: In-memory works for single gateway instance (free tier). In clustered prod, Redis is used.
@Component
public class LoginRateLimiterFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimiterFilter.class);
    private static final int MAX_REQUESTS_PER_MINUTE = 15;

    public static class RequestWindow {
        final long windowStartMs;
        final AtomicInteger count;

        public RequestWindow(long windowStartMs) {
            this.windowStartMs = windowStartMs;
            this.count = new AtomicInteger(1);
        }
    }

    private final Map<String, RequestWindow> ipWindows = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (!"/auth/login".equalsIgnoreCase(path) || !"POST".equalsIgnoreCase(exchange.getRequest().getMethod().name())) {
            return chain.filter(exchange);
        }

        String clientIp = resolveClientIp(exchange);
        long now = System.currentTimeMillis();

        RequestWindow window = ipWindows.compute(clientIp, (ip, current) -> {
            if (current == null || (now - current.windowStartMs) > 60000) {
                return new RequestWindow(now);
            }
            current.count.incrementAndGet();
            return current;
        });

        if (window.count.get() > MAX_REQUESTS_PER_MINUTE) {
            log.warn("Rate limit exceeded for IP '{}' on /auth/login", clientIp);
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            String errorJson = """
                    {
                      "timestamp": "%s",
                      "status": 429,
                      "code": "RATE_LIMIT_EXCEEDED",
                      "errorCode": "RATE_LIMIT_EXCEEDED",
                      "message": "Too many login attempts. Please wait a minute and try again.",
                      "path": "/auth/login"
                    }
                    """.formatted(Instant.now());
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(errorJson.getBytes(StandardCharsets.UTF_8));
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }

        return chain.filter(exchange);
    }

    private String resolveClientIp(ServerWebExchange exchange) {
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        if (exchange.getRequest().getRemoteAddress() != null) {
            return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }

    @Override
    public int getOrder() {
        return -2; // Run before JwtAuthenticationFilter (-1)
    }
}
