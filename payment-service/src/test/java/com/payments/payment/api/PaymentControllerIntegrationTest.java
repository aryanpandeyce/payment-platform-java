package com.payments.payment.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.payment.api.dto.InitiatePaymentRequest;
import com.payments.payment.domain.OutboxEvent;
import com.payments.payment.infrastructure.persistence.OutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OutboxRepository outboxRepository;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    @DisplayName("FR-5: POST /api/payments with new Idempotency-Key returns 202 Accepted and saves outbox event")
    void shouldInitiatePaymentAndSaveOutbox() throws Exception {
        String key = "key-" + UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        InitiatePaymentRequest req = new InitiatePaymentRequest(senderId, receiverId, new BigDecimal("500.00"), "INR");

        MvcResult result = mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.idempotencyKey", is(key)))
                .andExpect(jsonPath("$.status", is("INITIATED")))
                .andExpect(jsonPath("$.amount", is(500.00)))
                .andReturn();

        // Verify Outbox Event exists
        List<OutboxEvent> outboxList = outboxRepository.findAll();
        assertFalse(outboxList.isEmpty(), "Outbox event should be persisted in same transaction");
    }

    @Test
    @DisplayName("FR-5: Same Idempotency-Key with same body returns 200 OK and same payment")
    void shouldReturnExistingPaymentOnDuplicateRequest() throws Exception {
        String key = "idemp-dup-" + UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        InitiatePaymentRequest req = new InitiatePaymentRequest(senderId, receiverId, new BigDecimal("250.00"), "INR");

        // First call: 202 Accepted
        MvcResult firstResult = mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isAccepted())
                .andReturn();

        String firstPaymentId = objectMapper.readTree(firstResult.getResponse().getContentAsString()).get("id").asText();

        // Second call: 200 OK with same payment ID
        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(firstPaymentId)))
                .andExpect(jsonPath("$.status", is("INITIATED")));
    }

    @Test
    @DisplayName("FR-5: Same Idempotency-Key with different body returns 409 Conflict")
    void shouldRejectSameKeyWithDifferentPayload() throws Exception {
        String key = "idemp-mismatch-" + UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        InitiatePaymentRequest req1 = new InitiatePaymentRequest(senderId, receiverId, new BigDecimal("100.00"), "INR");
        InitiatePaymentRequest req2 = new InitiatePaymentRequest(senderId, receiverId, new BigDecimal("999.00"), "INR");

        // First call succeeds
        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isAccepted());

        // Second call with different amount -> 409 Conflict
        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("IDEMPOTENCY_PAYLOAD_MISMATCH")));
    }

    @Test
    @DisplayName("FR-5: Missing Idempotency-Key header returns 400 Bad Request")
    void shouldRequireIdempotencyKeyHeader() throws Exception {
        InitiatePaymentRequest req = new InitiatePaymentRequest(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"), "INR");

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("MISSING_IDEMPOTENCY_KEY")));
    }

    @Test
    @DisplayName("FR-6: GET /api/payments/{id} returns 200 OK for existing payment")
    void shouldGetPaymentById() throws Exception {
        String key = "idemp-get-" + UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        InitiatePaymentRequest req = new InitiatePaymentRequest(senderId, receiverId, new BigDecimal("75.00"), "INR");

        MvcResult result = mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isAccepted())
                .andReturn();

        String paymentId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/payments/" + paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(paymentId)))
                .andExpect(jsonPath("$.amount", is(75.00)));
    }
}
