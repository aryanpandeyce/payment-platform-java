package com.payments.gateway.controller;

import com.payments.gateway.dto.ErrorResponse;
import com.payments.gateway.dto.LoginRequest;
import com.payments.gateway.dto.LoginResponse;
import com.payments.gateway.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtUtil jwtUtil;
    private final String demoUsername;
    private final String demoPassword;

    public AuthController(
            JwtUtil jwtUtil,
            @Value("${auth.demo-user.username:user1}") String demoUsername,
            @Value("${auth.demo-user.password:password123}") String demoPassword) {
        this.jwtUtil = jwtUtil;
        this.demoUsername = demoUsername;
        this.demoPassword = demoPassword;
    }

    // FR-14: POST /auth/login - Issues signed JWT token for valid credentials
    @PostMapping("/login")
    public Mono<ResponseEntity<?>> login(@RequestBody LoginRequest request, ServerWebExchange exchange) {
        if (request.username() != null && request.password() != null &&
                request.username().equals(demoUsername) && request.password().equals(demoPassword)) {

            String token = jwtUtil.generateToken(request.username());
            LoginResponse response = new LoginResponse(token, "Bearer", 86400, request.username());
            return Mono.just(ResponseEntity.ok(response));
        }

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                "INVALID_CREDENTIALS",
                "Invalid username or password",
                exchange.getRequest().getPath().value()
        );
        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error));
    }
}
