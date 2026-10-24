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

    @KafkaListener(topics = "payment-initiated", groupId = "fraud-service-group")
    public void onPaymentInitiated(String message) {
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
                kafkaTemplate.send(TOPIC_VALIDATED, event.getPaymentId().toString(), payload);
                log.info("Fraud check PASSED. Published to '{}' for paymentId: {}", TOPIC_VALIDATED, event.getPaymentId());
            } else {
                PaymentFailedEvent failedEvent = new PaymentFailedEvent(
                        event.getPaymentId(),
                        result.getFailureReason()
                );
                String payload = objectMapper.writeValueAsString(failedEvent);
                kafkaTemplate.send(TOPIC_FAILED, event.getPaymentId().toString(), payload);
                log.warn("Fraud check FAILED ({}). Published to '{}' for paymentId: {}",
                        result.getFailureReason(), TOPIC_FAILED, event.getPaymentId());
            }
        } catch (Exception e) {
            log.error("Error processing payment-initiated event: {}", e.getMessage(), e);
        }
    }
}
