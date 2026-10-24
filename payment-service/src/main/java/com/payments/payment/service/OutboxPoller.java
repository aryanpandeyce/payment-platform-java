package com.payments.payment.service;

import com.payments.payment.domain.OutboxEvent;
import com.payments.payment.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// @Component: Outbox background poller
// Local DB Outbox table se pending events read karke Kafka topic par publish karta hai
@Component
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);
    private static final String TOPIC = "payment-initiated";

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPoller(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    // @Scheduled: Har 1 second mein run hota hai
    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void pollAndPublish() {
        List<OutboxEvent> pendingEvents = outboxRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING");
        if (pendingEvents.isEmpty()) {
            return;
        }

        for (OutboxEvent event : pendingEvents) {
            try {
                kafkaTemplate.send(TOPIC, event.getAggregateId().toString(), event.getPayload());
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
