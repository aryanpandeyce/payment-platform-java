package com.payments.payment.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.payment.api.dto.InitiatePaymentRequest;
import com.payments.payment.application.OutboxPoller;
import com.payments.payment.domain.OutboxEvent;
import com.payments.payment.infrastructure.persistence.OutboxRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class KafkaFlowTestcontainersTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("payment_flow_db")
            .withUsername("flowuser")
            .withPassword("flowpass");

    @Container
    static KafkaContainer kafka = new KafkaContainer("apache/kafka-native:3.8.1");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private OutboxPoller outboxPoller;

    @Test
    @DisplayName("Testcontainers: Outbox event published to Kafka broker with X-Correlation-Id header")
    void shouldPublishOutboxEventToRealKafka() throws Exception {
        String key = "flow-" + UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        InitiatePaymentRequest req = new InitiatePaymentRequest(senderId, receiverId, new BigDecimal("350.00"), "INR");

        // 1. Initiate payment
        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", key)
                        .header("X-Correlation-Id", "corr-test-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isAccepted());

        // 2. Outbox poller dispatches to Kafka
        outboxPoller.pollAndPublish();

        // 3. Verify event is marked SENT in Postgres
        List<OutboxEvent> outboxEvents = outboxRepository.findAll();
        assertFalse(outboxEvents.isEmpty());
        assertTrue(outboxEvents.stream().anyMatch(e -> "SENT".equals(e.getStatus())), "Outbox event should be marked SENT");

        // 4. Verify message exists on Kafka topic
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "testcontainers-verify-group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.subscribe(Collections.singletonList("payment-initiated"));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
            assertFalse(records.isEmpty(), "Should have consumed at least one message from payment-initiated topic");

            boolean found = false;
            for (ConsumerRecord<String, String> record : records) {
                if (record.value().contains(key)) {
                    found = true;
                    assertNotNull(record.headers().lastHeader("X-Correlation-Id"), "X-Correlation-Id header must be present on Kafka record");
                    break;
                }
            }
            assertTrue(found, "The initiated payment event must be found on Kafka topic");
        }
    }
}
