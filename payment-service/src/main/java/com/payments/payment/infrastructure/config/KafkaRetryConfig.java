package com.payments.payment.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

// Kafka Retry and Dead-Letter Topic (DLT) configuration:
// Retries failed consumer messages 2 times with a 1-second delay, then publishes to <topic>.DLT
@Configuration
public class KafkaRetryConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaRetryConfig.class);

    private final io.micrometer.core.instrument.MeterRegistry meterRegistry;

    public KafkaRetryConfig(io.micrometer.core.instrument.MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry != null ? meterRegistry : new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
    }

    public KafkaRetryConfig() {
        this(new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
    }

    @Bean
    public CommonErrorHandler kafkaCommonErrorHandler(KafkaOperations<Object, Object> template) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
                (record, ex) -> {
                    log.error("Failed message on topic '{}' after retries. Forwarding to DLT. Error: {}",
                            record.topic(), ex.getMessage());
                    meterRegistry.counter("kafka_dlt_messages_total").increment();
                    return new org.apache.kafka.common.TopicPartition(record.topic() + ".DLT", -1);
                });

        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2L));
    }
}
