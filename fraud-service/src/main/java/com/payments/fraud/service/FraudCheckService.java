package com.payments.fraud.service;

import com.payments.fraud.config.FraudProperties;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// @Service: Rule-based fraud detection engine
@Service
public class FraudCheckService {

    private final FraudProperties properties;
    private final io.micrometer.core.instrument.MeterRegistry meterRegistry;
    // In-memory sliding window cache: senderAccountId -> Deque of timestamps
    private final Map<UUID, Deque<Instant>> senderTransferTimestamps = new ConcurrentHashMap<>();

    public FraudCheckService(FraudProperties properties) {
        this(properties, new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
    }

    public FraudCheckService(FraudProperties properties, io.micrometer.core.instrument.MeterRegistry meterRegistry) {
        this.properties = properties;
        this.meterRegistry = meterRegistry != null ? meterRegistry : new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
    }

    public synchronized FraudCheckResult evaluate(UUID senderAccountId, BigDecimal amount) {
        Instant now = Instant.now();

        // FR-8: Maximum amount limit check (Default ₹50,000.00)
        if (amount != null && amount.compareTo(properties.getMaxAmount()) > 0) {
            meterRegistry.counter("fraud_rejections_total", "rule", "EXCEEDS_MAX_AMOUNT_LIMIT").increment();
            return FraudCheckResult.fail("EXCEEDS_MAX_AMOUNT_LIMIT");
        }

        // FR-9: Transfer velocity check (Default 5 transfers per 60 seconds)
        Deque<Instant> timestamps = senderTransferTimestamps.computeIfAbsent(senderAccountId, k -> new ArrayDeque<>());

        // 60 second se puraane timestamps prune karo
        Instant oneMinuteAgo = now.minus(Duration.ofSeconds(60));
        while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(oneMinuteAgo)) {
            timestamps.pollFirst();
        }

        // Agar sliding window mein 5 transfers already ho chuke hain, toh 6th reject hoga
        if (timestamps.size() >= properties.getMaxVelocityPerMinute()) {
            meterRegistry.counter("fraud_rejections_total", "rule", "VELOCITY_LIMIT_EXCEEDED").increment();
            return FraudCheckResult.fail("VELOCITY_LIMIT_EXCEEDED");
        }

        // Rules pass ho gaye -> timestamp record karo
        timestamps.addLast(now);
        return FraudCheckResult.pass();
    }

    // Testing ke liye cache reset helper
    public void reset() {
        senderTransferTimestamps.clear();
    }
}
