package com.payments.payment.application;

import com.payments.payment.domain.OutboxEvent;
import com.payments.payment.infrastructure.persistence.OutboxRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

// Outbox background poller: polls local DB outbox table and publishes events to Kafka with correlation ID
@Component
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);
    private static final String TOPIC = "payment-initiated";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPoller(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void pollAndPublish() {
        List<OutboxEvent> pendingEvents = outboxRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING");
        if (pendingEvents.isEmpty()) {
            return;
        }

        for (OutboxEvent event : pendingEvents) {
            String correlationId = MDC.get("correlationId");
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }

            try {
                ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, event.getAggregateId().toString(), event.getPayload());
                record.headers().add(CORRELATION_ID_HEADER, correlationId.getBytes(StandardCharsets.UTF_8));

                kafkaTemplate.send(record);
                event.markSent();
                outboxRepository.save(event);
                log.info("Outbox event published to Kafka topic '{}': {}", TOPIC, event.getId());
            } catch (Exception e) {
                log.warn("Failed to publish outbox event {} to Kafka: {}", event.getId(), e.getMessage());
                break;
            }
        }
    }
}
