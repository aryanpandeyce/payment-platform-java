package com.payments.payment.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.payment.api.dto.InitiatePaymentRequest;
import com.payments.payment.api.dto.PaymentResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentRbacIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("FR-21: ADMIN cannot initiate payment transfer (403 Forbidden)")
    void adminCannotInitiatePayment() throws Exception {
        InitiatePaymentRequest request = new InitiatePaymentRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("500.00"),
                "INR"
        );

        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .header("X-User-Role", "ADMIN")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("FR-21: USER A can initiate payment; USER B cannot access USER A's payment")
    void userIsolation_UserBCannotAccessUserAPayment() throws Exception {
        UUID userAId = UUID.randomUUID();
        UUID userBId = UUID.randomUUID();
        UUID senderAccountId = UUID.randomUUID();
        UUID receiverAccountId = UUID.randomUUID();

        InitiatePaymentRequest request = new InitiatePaymentRequest(
                senderAccountId,
                receiverAccountId,
                new BigDecimal("1000.00"),
                "INR"
        );

        // 1. User A initiates payment -> 202 Accepted
        MvcResult result = mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .header("X-User-Id", userAId.toString())
                        .header("X-User-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andReturn();

        PaymentResponse payment = objectMapper.readValue(result.getResponse().getContentAsString(), PaymentResponse.class);
        UUID paymentId = payment.id();

        // 2. User B attempts to access User A's payment -> 403 Forbidden
        mockMvc.perform(get("/api/payments/{id}", paymentId)
                        .header("X-User-Id", userBId.toString())
                        .header("X-User-Role", "USER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));

        // 3. User B attempts to view timeline of User A's payment -> 403 Forbidden
        mockMvc.perform(get("/api/payments/{id}/timeline", paymentId)
                        .header("X-User-Id", userBId.toString())
                        .header("X-User-Role", "USER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));

        // 4. User A reads own payment -> 200 OK
        mockMvc.perform(get("/api/payments/{id}", paymentId)
                        .header("X-User-Id", userAId.toString())
                        .header("X-User-Role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId.toString()));

        // 5. Admin can inspect User A's payment -> 200 OK
        mockMvc.perform(get("/api/payments/{id}", paymentId)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId.toString()));
    }
}
