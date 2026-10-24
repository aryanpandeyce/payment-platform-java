package com.payments.payment.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.payment.api.dto.InitiatePaymentRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentFeaturesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should initiate payment, record timeline, and support search and pagination")
    void testTimelineSearchAndPagination() throws Exception {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        String idempotencyKey = "timeline-test-" + UUID.randomUUID();

        InitiatePaymentRequest request = new InitiatePaymentRequest(
                senderId,
                receiverId,
                new BigDecimal("750.00"),
                "INR"
        );

        MvcResult result = mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andReturn();

        String paymentId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        // 1. Verify timeline endpoint
        mockMvc.perform(get("/api/payments/{id}/timeline", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].status", is("INITIATED")));

        // 2. Verify search by ID
        mockMvc.perform(get("/api/payments")
                        .param("search", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(paymentId)));

        // 3. Verify pagination
        mockMvc.perform(get("/api/payments")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));

        // 4. Verify filter by status
        mockMvc.perform(get("/api/payments")
                        .param("status", "INITIATED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].status", is("INITIATED")));
    }
}
