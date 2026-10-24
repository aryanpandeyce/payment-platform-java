package com.payments.gateway.controller;

import com.payments.gateway.dto.LoginRequest;
import com.payments.gateway.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class AuthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("FR-14: POST /auth/login with valid demo credentials returns 200 OK and JWT token")
    void shouldLoginSuccessfully() {
        LoginRequest req = new LoginRequest("admin", "password");

        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.token").isNotEmpty()
                .jsonPath("$.tokenType").isEqualTo("Bearer")
                .jsonPath("$.username").isEqualTo("admin");
    }

    @Test
    @DisplayName("FR-14: POST /auth/login with invalid credentials returns 401 Unauthorized")
    void shouldRejectInvalidCredentials() {
        LoginRequest req = new LoginRequest("admin", "wrongpassword");

        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("INVALID_CREDENTIALS");
    }
}
