package com.payments.fraud.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.fraud.event.PaymentFailedEvent;
import com.payments.fraud.event.PaymentInitiatedEvent;
import com.payments.fraud.event.PaymentValidatedEvent;
import com.payments.fraud.service.FraudCheckResult;
import com.payments.fraud.service.FraudCheckService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// @Component: Kafka listener jo payment-initiated events sunta hai aur validation result publish karta hai
@Component
public class FraudEventListener {

    private static final Logger log = LoggerFactory.getLogger(FraudEventListener.class);
    private static final String TOPIC_VALIDATED = "payment-validated";
    private static final String TOPIC_FAILED = "payment-failed";

    private final FraudCheckService fraudCheckService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // Idempotent consumer: duplicate events ko dobara process na karne ke liye cache
    private final Set<UUID> processedPayments = ConcurrentHashMap.newKeySet();

    public FraudEventListener(FraudCheckService fraudCheckService,
                              KafkaTemplate<String, String> kafkaTemplate,
                              ObjectMapper objectMapper) {
        this.fraudCheckService = fraudCheckService;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @KafkaListener(topics = "payment-initiated", groupId = "fraud-service-group")
    public void onPaymentInitiated(@org.springframework.messaging.handler.annotation.Payload String message,
                                   @org.springframework.messaging.handler.annotation.Header(name = CORRELATION_ID_HEADER, required = false) byte[] correlationIdBytes) {
        String correlationId = (correlationIdBytes != null && correlationIdBytes.length > 0)
                ? new String(correlationIdBytes, java.nio.charset.StandardCharsets.UTF_8)
                : UUID.randomUUID().toString();

        org.slf4j.MDC.put("correlationId", correlationId);
        try {
            PaymentInitiatedEvent event = objectMapper.readValue(message, PaymentInitiatedEvent.class);

            // Idempotency check: agar already evaluate ho chuka hai toh ignore karo
            if (!processedPayments.add(event.getPaymentId())) {
                log.info("Duplicate payment-initiated event ignored for paymentId: {}", event.getPaymentId());
                return;
            }

            log.info("Evaluating fraud rules for paymentId: {}, amount: {}", event.getPaymentId(), event.getAmount());
            FraudCheckResult result = fraudCheckService.evaluate(event.getSenderAccountId(), event.getAmount());

            if (result.isPassed()) {
                PaymentValidatedEvent validatedEvent = new PaymentValidatedEvent(
                        event.getPaymentId(),
                        event.getSenderAccountId(),
                        event.getReceiverAccountId(),
                        event.getAmount(),
                        event.getCurrency()
                );
                String payload = objectMapper.writeValueAsString(validatedEvent);
                sendWithCorrelation(TOPIC_VALIDATED, event.getPaymentId().toString(), payload, correlationId);
                log.info("Fraud check PASSED. Published to '{}' for paymentId: {}", TOPIC_VALIDATED, event.getPaymentId());
            } else {
                PaymentFailedEvent failedEvent = new PaymentFailedEvent(
                        event.getPaymentId(),
                        result.getFailureReason()
                );
                String payload = objectMapper.writeValueAsString(failedEvent);
                sendWithCorrelation(TOPIC_FAILED, event.getPaymentId().toString(), payload, correlationId);
                log.warn("Fraud check FAILED ({}). Published to '{}' for paymentId: {}",
                        result.getFailureReason(), TOPIC_FAILED, event.getPaymentId());
            }
        } catch (Exception e) {
            log.error("Error processing payment-initiated event: {}", e.getMessage(), e);
        } finally {
            org.slf4j.MDC.remove("correlationId");
        }
    }

    private void sendWithCorrelation(String topic, String key, String payload, String correlationId) {
        org.apache.kafka.clients.producer.ProducerRecord<String, String> record = new org.apache.kafka.clients.producer.ProducerRecord<>(topic, key, payload);
        if (correlationId != null) {
            record.headers().add(CORRELATION_ID_HEADER, correlationId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        kafkaTemplate.send(record);
    }
}

