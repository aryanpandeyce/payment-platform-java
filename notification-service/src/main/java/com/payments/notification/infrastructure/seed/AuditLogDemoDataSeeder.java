package com.payments.notification.infrastructure.seed;

import com.payments.notification.domain.AuditLog;
import com.payments.notification.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Seeds matching audit trail logs in MongoDB for demo payments
@Component
@ConditionalOnProperty(name = "seed.demo.enabled", havingValue = "true", matchIfMissing = true)
public class AuditLogDemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AuditLogDemoDataSeeder.class);
    private final AuditLogRepository auditLogRepository;

    public AuditLogDemoDataSeeder(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void run(String... args) {
        try {
            if (auditLogRepository.count() > 0) {
                log.info("Audit logs collection is not empty; skipping demo audit seed");
                return;
            }

            log.info("Seeding demo audit logs into MongoDB audit_db...");
            Instant now = Instant.now();
            List<AuditLog> logs = new ArrayList<>();

            // Seed historical PaymentSettled and PaymentFailed audit logs
            for (int i = 1; i <= 25; i++) {
                Instant time = now.minus(30 - i, ChronoUnit.DAYS);
                String paymentId = "p" + String.format("%07d", i) + "-0000-0000-0000-000000000001";
                String correlationId = UUID.randomUUID().toString();

                logs.add(new AuditLog(
                        UUID.randomUUID().toString(),
                        "PaymentInitiated",
                        paymentId,
                        time,
                        "{\"amount\":" + (500 * i) + ".00,\"currency\":\"INR\"}",
                        correlationId
                ));

                if (i % 5 == 0) {
                    logs.add(new AuditLog(
                            UUID.randomUUID().toString(),
                            "PaymentFailed",
                            paymentId,
                            time.plus(1, ChronoUnit.SECONDS),
                            "{\"failureReason\":\"INSUFFICIENT_FUNDS\"}",
                            correlationId
                    ));
                } else {
                    logs.add(new AuditLog(
                            UUID.randomUUID().toString(),
                            "PaymentValidated",
                            paymentId,
                            time.plus(500, ChronoUnit.MILLIS),
                            "{\"status\":\"VALIDATED\"}",
                            correlationId
                    ));
                    logs.add(new AuditLog(
                            UUID.randomUUID().toString(),
                            "PaymentSettled",
                            paymentId,
                            time.plus(1, ChronoUnit.SECONDS),
                            "{\"status\":\"SETTLED\"}",
                            correlationId
                    ));
                }
            }

            auditLogRepository.saveAll(logs);
            log.info("Successfully seeded {} immutable audit log documents into MongoDB", logs.size());
        } catch (Exception e) {
            log.warn("Could not seed MongoDB audit logs (MongoDB may be offline in test mode): {}", e.getMessage());
        }
    }
}
