package com.payments.account.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.account.application.AccountService;
import com.payments.account.domain.AccountNotFoundException;
import com.payments.account.domain.InsufficientFundsException;
import com.payments.account.infrastructure.kafka.event.PaymentFailedEvent;
import com.payments.account.infrastructure.kafka.event.PaymentSettledEvent;
import com.payments.account.infrastructure.kafka.event.PaymentValidatedEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Kafka listener jo payment-validated events consume karta hai aur atomic settlement execute karta hai
@Component
public class SettlementEventListener {

    private static final Logger log = LoggerFactory.getLogger(SettlementEventListener.class);
    private static final String TOPIC_SETTLED = "payment-settled";
    private static final String TOPIC_FAILED = "payment-failed";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    private final AccountService accountService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // Idempotent consumer: duplicate events ignore karne ke liye in-memory cache
    private final Set<UUID> processedPayments = ConcurrentHashMap.newKeySet();

    public SettlementEventListener(AccountService accountService,
                                   KafkaTemplate<String, String> kafkaTemplate,
                                   ObjectMapper objectMapper) {
        this.accountService = accountService;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "payment-validated", groupId = "account-service-group")
    public void onPaymentValidated(@Payload String message,
                                   @Header(name = CORRELATION_ID_HEADER, required = false) byte[] correlationIdBytes) {
        String correlationId = (correlationIdBytes != null && correlationIdBytes.length > 0)
                ? new String(correlationIdBytes, StandardCharsets.UTF_8)
                : UUID.randomUUID().toString();

        MDC.put("correlationId", correlationId);
        try {
            PaymentValidatedEvent event = objectMapper.readValue(message, PaymentValidatedEvent.class);

            // Idempotency check: duplicate event skip
            if (!processedPayments.add(event.paymentId())) {
                log.info("Duplicate payment-validated event ignored for paymentId: {}", event.paymentId());
                return;
            }

            log.info("Executing settlement for paymentId: {}, sender: {}, receiver: {}, amount: {}",
                    event.paymentId(), event.senderAccountId(), event.receiverAccountId(), event.amount());

            try {
                // FR-10: Atomic transfer in ONE database transaction with paymentId reference
                accountService.transfer(event.senderAccountId(), event.receiverAccountId(), event.amount(), event.paymentId().toString());

                // Settlement successful
                PaymentSettledEvent settledEvent = new PaymentSettledEvent(
                        event.paymentId(),
                        event.senderAccountId(),
                        event.receiverAccountId(),
                        event.amount(),
                        event.currency()
                );
                sendWithCorrelation(TOPIC_SETTLED, event.paymentId().toString(), objectMapper.writeValueAsString(settledEvent), correlationId);
                log.info("Settlement SUCCESS. Published to '{}' for paymentId: {}", TOPIC_SETTLED, event.paymentId());

            } catch (InsufficientFundsException e) {
                log.warn("Settlement FAILED for paymentId: {} due to insufficient funds", event.paymentId());
                publishFailure(event.paymentId(), "INSUFFICIENT_FUNDS", correlationId);

            } catch (AccountNotFoundException e) {
                log.warn("Settlement FAILED for paymentId: {} due to missing account: {}", event.paymentId(), e.getMessage());
                publishFailure(event.paymentId(), "RECEIVER_CREDIT_FAILED_REFUNDED", correlationId);

            } catch (Exception e) {
                log.error("Settlement FAILED for paymentId: {} with unexpected error: {}", event.paymentId(), e.getMessage());
                publishFailure(event.paymentId(), "SETTLEMENT_FAILED", correlationId);
            }

        } catch (Exception e) {
            log.error("Error processing payment-validated event: {}", e.getMessage(), e);
        } finally {
            MDC.remove("correlationId");
        }
    }

    private void publishFailure(UUID paymentId, String reason, String correlationId) {
        try {
            PaymentFailedEvent failedEvent = new PaymentFailedEvent(paymentId, reason);
            sendWithCorrelation(TOPIC_FAILED, paymentId.toString(), objectMapper.writeValueAsString(failedEvent), correlationId);
            log.info("Published settlement failure to '{}' for paymentId: {}", TOPIC_FAILED, paymentId);
        } catch (Exception ex) {
            log.error("Failed to publish settlement failure event for paymentId: {}", paymentId, ex);
        }
    }

    private void sendWithCorrelation(String topic, String key, String payload, String correlationId) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, payload);
        if (correlationId != null) {
            record.headers().add(CORRELATION_ID_HEADER, correlationId.getBytes(StandardCharsets.UTF_8));
        }
        kafkaTemplate.send(record);
    }
}
