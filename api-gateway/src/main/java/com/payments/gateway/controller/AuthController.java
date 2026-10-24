package com.payments.gateway.controller;

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
import reactor.core.publisher.Mono;

import java.util.Map;

// @RestController: Reactive WebFlux login endpoint
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtUtil jwtUtil;
    private final String demoUsername;
    private final String demoPassword;

    public AuthController(
            JwtUtil jwtUtil,
            @Value("${auth.demo-user.username:admin}") String demoUsername,
            @Value("${auth.demo-user.password:password}") String demoPassword) {
        this.jwtUtil = jwtUtil;
        this.demoUsername = demoUsername;
        this.demoPassword = demoPassword;
    }

    // FR-14: POST /auth/login - Issues signed JWT token for valid credentials
    @PostMapping("/login")
    public Mono<ResponseEntity<?>> login(@RequestBody LoginRequest request) {
        if (request.getUsername() != null && request.getPassword() != null &&
                request.getUsername().equals(demoUsername) && request.getPassword().equals(demoPassword)) {

            String token = jwtUtil.generateToken(request.getUsername());
            LoginResponse response = new LoginResponse(token, "Bearer", 86400, request.getUsername());
            return Mono.just(ResponseEntity.ok(response));
        }

        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("errorCode", "INVALID_CREDENTIALS", "message", "Invalid username or password")));
    }
}
