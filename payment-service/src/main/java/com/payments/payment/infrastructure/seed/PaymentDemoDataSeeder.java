package com.payments.payment.infrastructure.seed;

import com.payments.payment.domain.Payment;
import com.payments.payment.domain.PaymentStatus;
import com.payments.payment.domain.PaymentTimeline;
import com.payments.payment.infrastructure.persistence.PaymentRepository;
import com.payments.payment.infrastructure.persistence.PaymentTimelineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

// Seeds ~40 realistic historical payments across the last 30 days
// Mix of SETTLED, FAILED (fraud limit, velocity, insufficient funds)
@Component
@ConditionalOnProperty(name = "seed.demo.enabled", havingValue = "true", matchIfMissing = true)
public class PaymentDemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PaymentDemoDataSeeder.class);

    // Standard User UUIDs
    public static final UUID RAHUL_ID = UUID.fromString("00000000-0000-0000-0000-000000000011");
    public static final UUID PRIYA_ID = UUID.fromString("00000000-0000-0000-0000-000000000012");
    public static final UUID ANKIT_ID = UUID.fromString("00000000-0000-0000-0000-000000000013");
    public static final UUID SNEHA_ID = UUID.fromString("00000000-0000-0000-0000-000000000014");
    public static final UUID FAIZ_ID = UUID.fromString("00000000-0000-0000-0000-000000000015");
    public static final UUID KAVYA_ID = UUID.fromString("00000000-0000-0000-0000-000000000016");
    public static final UUID VIKRAM_ID = UUID.fromString("00000000-0000-0000-0000-000000000017");
    public static final UUID ANANYA_ID = UUID.fromString("00000000-0000-0000-0000-000000000018");

    // Standard Account UUIDs
    public static final UUID RAHUL_ACC_1 = UUID.fromString("a1000000-0000-0000-0000-000000000011");
    public static final UUID RAHUL_ACC_2 = UUID.fromString("a1000000-0000-0000-0000-000000000021");
    public static final UUID PRIYA_ACC_1 = UUID.fromString("a1000000-0000-0000-0000-000000000012");
    public static final UUID ANKIT_ACC_1 = UUID.fromString("a1000000-0000-0000-0000-000000000013");
    public static final UUID ANKIT_ACC_2 = UUID.fromString("a1000000-0000-0000-0000-000000000023");
    public static final UUID SNEHA_ACC_1 = UUID.fromString("a1000000-0000-0000-0000-000000000014");
    public static final UUID FAIZ_ACC_1 = UUID.fromString("a1000000-0000-0000-0000-000000000015");
    public static final UUID KAVYA_ACC_1 = UUID.fromString("a1000000-0000-0000-0000-000000000016");
    public static final UUID VIKRAM_ACC_1 = UUID.fromString("a1000000-0000-0000-0000-000000000017");
    public static final UUID ANANYA_ACC_1 = UUID.fromString("a1000000-0000-0000-0000-000000000018");

    private final PaymentRepository paymentRepository;
    private final PaymentTimelineRepository timelineRepository;

    public PaymentDemoDataSeeder(PaymentRepository paymentRepository, PaymentTimelineRepository timelineRepository) {
        this.paymentRepository = paymentRepository;
        this.timelineRepository = timelineRepository;
    }

    @Override
    public void run(String... args) {
        if (paymentRepository.count() > 0) {
            log.info("Payments table is not empty; skipping payment demo seed");
            return;
        }

        log.info("Seeding ~40 historical payments spread across 30 days...");
        Instant now = Instant.now();

        // 1. SETTLED Payments (Realistic transfers between users)
        createPayment(now.minus(28, ChronoUnit.DAYS), RAHUL_ID, RAHUL_ACC_1, PRIYA_ACC_1, new BigDecimal("500.00"), PaymentStatus.SETTLED, null);
        createPayment(now.minus(27, ChronoUnit.DAYS), PRIYA_ID, PRIYA_ACC_1, SNEHA_ACC_1, new BigDecimal("1200.00"), PaymentStatus.SETTLED, null);
        createPayment(now.minus(26, ChronoUnit.DAYS), ANKIT_ID, ANKIT_ACC_1, RAHUL_ACC_1, new BigDecimal("2500.00"), PaymentStatus.SETTLED, null);
        createPayment(now.minus(25, ChronoUnit.DAYS), FAIZ_ID, FAIZ_ACC_1, ANKIT_ACC_1, new BigDecimal("15000.00"), PaymentStatus.SETTLED, null);
        createPayment(now.minus(24, ChronoUnit.DAYS), SNEHA_ID, SNEHA_ACC_1, KAVYA_ACC_1, new BigDecimal("3400.00"), PaymentStatus.SETTLED, null);
        createPayment(now.minus(23, ChronoUnit.DAYS), KAVYA_ID, KAVYA_ACC_1, ANANYA_ACC_1, new BigDecimal("850.00"), PaymentStatus.SETTLED, null);
        createPayment(now.minus(22, ChronoUnit.DAYS), ANANYA_ID, ANANYA_ACC_1, RAHUL_ACC_1, new BigDecimal("4500.00"), PaymentStatus.SETTLED, null);
        createPayment(now.minus(21, ChronoUnit.DAYS), RAHUL_ID, RAHUL_ACC_2, ANKIT_ACC_2, new BigDecimal("12000.00"), PaymentStatus.SETTLED, null);
        createPayment(now.minus(20, ChronoUnit.DAYS), PRIYA_ID, PRIYA_ACC_1, FAIZ_ACC_1, new BigDecimal("6000.00"), PaymentStatus.SETTLED, null);
        createPayment(now.minus(19, ChronoUnit.DAYS), ANKIT_ID, ANKIT_ACC_2, SNEHA_ACC_1, new BigDecimal("7500.00"), PaymentStatus.SETTLED, null);
        createPayment(now.minus(18, ChronoUnit.DAYS), SNEHA_ID, SNEHA_ACC_1, PRIYA_ACC_1, new BigDecimal("1800.00"), PaymentStatus.SETTLED, null);
        createPayment(now.minus(17, ChronoUnit.DAYS), FAIZ_ID, FAIZ_ACC_1, RAHUL_ACC_1, new BigDecimal("22000.00"), PaymentStatus.SETTLED, null);
        createPayment(now.minus(16, ChronoUnit.DAYS), KAVYA_ID, KAVYA_ACC_1, SNEHA_ACC_1, new BigDecimal("1100.00"), PaymentStatus.SETTLED, null);
        createPayment(now.minus(15, ChronoUnit.DAYS), ANANYA_ID, ANANYA_ACC_1, PRIYA_ACC_1, new BigDecimal("3200.00"), PaymentStatus.SETTLED, null);
        createPayment(now.minus(14, ChronoUnit.DAYS), RAHUL_ID, RAHUL_ACC_1, KAVYA_ACC_1, new BigDecimal("950.00"), PaymentStatus.SETTLED, null);
        createPayment(now.minus(13, ChronoUnit.DAYS), PRIYA_ID, PRIYA_ACC_1, ANKIT_ACC_1, new BigDecimal("4100.00"), PaymentStatus.SETTLED, null);
        createPayment(now.minus(12, ChronoUnit.DAYS), ANKIT_ID, ANKIT_ACC_1, FAIZ_ACC_1, new BigDecimal("5500.00"), PaymentStatus.SETTLED, null);
        createPayment(now.minus(11, ChronoUnit.DAYS), SNEHA_ID, SNEHA_ACC_1, ANANYA_ACC_1, new BigDecimal("2700.00"), PaymentStatus.SETTLED, null);
        createPayment(now.minus(10, ChronoUnit.DAYS), FAIZ_ID, FAIZ_ACC_1, PRIYA_ACC_1, new BigDecimal("18500.00"), PaymentStatus.SETTLED, null);
        createPayment(now.minus(9, ChronoUnit.DAYS), KAVYA_ID, KAVYA_ACC_1, RAHUL_ACC_1, new BigDecimal("1450.00"), PaymentStatus.SETTLED, null);
        createPayment(now.minus(8, ChronoUnit.DAYS), ANANYA_ID, ANANYA_ACC_1, ANKIT_ACC_1, new BigDecimal("6300.00"), PaymentStatus.SETTLED, null);
        createPayment(now.minus(7, ChronoUnit.DAYS), RAHUL_ID, RAHUL_ACC_1, PRIYA_ACC_1, new BigDecimal("750.00"), PaymentStatus.SETTLED, null);
        createPayment(now.minus(6, ChronoUnit.DAYS), PRIYA_ID, PRIYA_ACC_1, SNEHA_ACC_1, new BigDecimal("2100.00"), PaymentStatus.SETTLED, null);
        createPayment(now.minus(5, ChronoUnit.DAYS), ANKIT_ID, ANKIT_ACC_1, KAVYA_ACC_1, new BigDecimal("3800.00"), PaymentStatus.SETTLED, null);
        createPayment(now.minus(4, ChronoUnit.DAYS), SNEHA_ID, SNEHA_ACC_1, RAHUL_ACC_1, new BigDecimal("1900.00"), PaymentStatus.SETTLED, null);
        createPayment(now.minus(3, ChronoUnit.DAYS), FAIZ_ID, FAIZ_ACC_1, ANANYA_ACC_1, new BigDecimal("25000.00"), PaymentStatus.SETTLED, null);
        createPayment(now.minus(2, ChronoUnit.DAYS), KAVYA_ID, KAVYA_ACC_1, PRIYA_ACC_1, new BigDecimal("980.00"), PaymentStatus.SETTLED, null);
        createPayment(now.minus(1, ChronoUnit.DAYS), ANANYA_ID, ANANYA_ACC_1, RAHUL_ACC_1, new BigDecimal("4200.00"), PaymentStatus.SETTLED, null);
        createPayment(now.minus(12, ChronoUnit.HOURS), RAHUL_ID, RAHUL_ACC_1, ANKIT_ACC_1, new BigDecimal("1500.00"), PaymentStatus.SETTLED, null);
        createPayment(now.minus(6, ChronoUnit.HOURS), PRIYA_ID, PRIYA_ACC_1, KAVYA_ACC_1, new BigDecimal("800.00"), PaymentStatus.SETTLED, null);
        createPayment(now.minus(2, ChronoUnit.HOURS), ANKIT_ID, ANKIT_ACC_1, SNEHA_ACC_1, new BigDecimal("2200.00"), PaymentStatus.SETTLED, null);
        createPayment(now.minus(30, ChronoUnit.MINUTES), SNEHA_ID, SNEHA_ACC_1, FAIZ_ACC_1, new BigDecimal("3500.00"), PaymentStatus.SETTLED, null);

        // 2. FAILED Payments (Diverse failure reasons)
        // a. EXCEEDS_MAX_AMOUNT_LIMIT (> ₹50,000 threshold)
        createPayment(now.minus(20, ChronoUnit.DAYS), RAHUL_ID, RAHUL_ACC_1, PRIYA_ACC_1, new BigDecimal("65000.00"), PaymentStatus.FAILED, "EXCEEDS_MAX_AMOUNT_LIMIT");
        createPayment(now.minus(10, ChronoUnit.DAYS), ANKIT_ID, ANKIT_ACC_1, FAIZ_ACC_1, new BigDecimal("75000.00"), PaymentStatus.FAILED, "EXCEEDS_MAX_AMOUNT_LIMIT");

        // b. VELOCITY_LIMIT_EXCEEDED (> 5 transfers/min)
        createPayment(now.minus(15, ChronoUnit.DAYS), PRIYA_ID, PRIYA_ACC_1, RAHUL_ACC_1, new BigDecimal("500.00"), PaymentStatus.FAILED, "VELOCITY_LIMIT_EXCEEDED");
        createPayment(now.minus(8, ChronoUnit.DAYS), SNEHA_ID, SNEHA_ACC_1, KAVYA_ACC_1, new BigDecimal("600.00"), PaymentStatus.FAILED, "VELOCITY_LIMIT_EXCEEDED");

        // c. INSUFFICIENT_FUNDS (Attempting to transfer more than balance)
        createPayment(now.minus(16, ChronoUnit.DAYS), VIKRAM_ID, VIKRAM_ACC_1, RAHUL_ACC_1, new BigDecimal("25000.00"), PaymentStatus.FAILED, "INSUFFICIENT_FUNDS");
        createPayment(now.minus(5, ChronoUnit.DAYS), KAVYA_ID, KAVYA_ACC_1, FAIZ_ACC_1, new BigDecimal("45000.00"), PaymentStatus.FAILED, "INSUFFICIENT_FUNDS");
        createPayment(now.minus(1, ChronoUnit.DAYS), VIKRAM_ID, VIKRAM_ACC_1, PRIYA_ACC_1, new BigDecimal("10000.00"), PaymentStatus.FAILED, "INSUFFICIENT_FUNDS");
        createPayment(now.minus(3, ChronoUnit.HOURS), ANKIT_ID, ANKIT_ACC_1, RAHUL_ACC_1, new BigDecimal("99000.00"), PaymentStatus.FAILED, "INSUFFICIENT_FUNDS");

        log.info("Successfully seeded 40 historical payments (32 SETTLED, 8 FAILED)");
    }

    private void createPayment(Instant timestamp, UUID userId, UUID senderAccountId, UUID receiverAccountId, BigDecimal amount, PaymentStatus status, String failureReason) {
        String key = "seed-" + UUID.randomUUID();
        Payment payment = new Payment(key, senderAccountId, receiverAccountId, amount, "INR", userId);

        try {
            Field statusField = Payment.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(payment, status);

            Field failureField = Payment.class.getDeclaredField("failureReason");
            failureField.setAccessible(true);
            failureField.set(payment, failureReason);

            Field createdField = Payment.class.getDeclaredField("createdAt");
            createdField.setAccessible(true);
            createdField.set(payment, timestamp);

            Field updatedField = Payment.class.getDeclaredField("updatedAt");
            updatedField.setAccessible(true);
            updatedField.set(payment, timestamp.plus(1, ChronoUnit.SECONDS));
        } catch (Exception e) {
            log.error("Failed to set payment fields via reflection", e);
        }

        Payment saved = paymentRepository.save(payment);

        // Timeline: INITIATED
        saveTimeline(saved.getId(), PaymentStatus.INITIATED, timestamp, null);

        if (status == PaymentStatus.SETTLED) {
            saveTimeline(saved.getId(), PaymentStatus.VALIDATED, timestamp.plus(500, ChronoUnit.MILLIS), null);
            saveTimeline(saved.getId(), PaymentStatus.SETTLED, timestamp.plus(1, ChronoUnit.SECONDS), null);
        } else if (status == PaymentStatus.FAILED) {
            if ("INSUFFICIENT_FUNDS".equals(failureReason)) {
                saveTimeline(saved.getId(), PaymentStatus.VALIDATED, timestamp.plus(500, ChronoUnit.MILLIS), null);
            }
            saveTimeline(saved.getId(), PaymentStatus.FAILED, timestamp.plus(1, ChronoUnit.SECONDS), failureReason);
        }
    }

    private void saveTimeline(UUID paymentId, PaymentStatus status, Instant timestamp, String failureReason) {
        PaymentTimeline entry = new PaymentTimeline(paymentId, status, timestamp, failureReason);
        timelineRepository.save(entry);
    }
}
