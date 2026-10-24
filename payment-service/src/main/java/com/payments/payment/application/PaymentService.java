package com.payments.payment.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.payment.api.dto.InitiatePaymentRequest;
import com.payments.payment.api.dto.PaymentResponse;
import com.payments.payment.api.dto.TimelineEntryResponse;
import com.payments.payment.domain.*;
import com.payments.payment.infrastructure.kafka.event.PaymentInitiatedEvent;
import com.payments.payment.infrastructure.persistence.OutboxRepository;
import com.payments.payment.infrastructure.persistence.PaymentRepository;
import com.payments.payment.infrastructure.persistence.PaymentTimelineRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

// Application Service: Coordinates idempotent payments, status lifecycle, timeline, and transactional outbox persistence
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxRepository outboxRepository;
    private final PaymentTimelineRepository paymentTimelineRepository;
    private final ObjectMapper objectMapper;
    private final io.micrometer.core.instrument.MeterRegistry meterRegistry;
    private final io.micrometer.core.instrument.Counter paymentsInitiatedCounter;
    private final io.micrometer.core.instrument.Counter paymentsSettledCounter;
    private final io.micrometer.core.instrument.Timer settlementTimer;

    public record InitiationResult(PaymentResponse response, boolean isNew) {}

    public PaymentService(PaymentRepository paymentRepository,
                          OutboxRepository outboxRepository,
                          PaymentTimelineRepository paymentTimelineRepository,
                          ObjectMapper objectMapper,
                          io.micrometer.core.instrument.MeterRegistry meterRegistry) {
        this.paymentRepository = paymentRepository;
        this.outboxRepository = outboxRepository;
        this.paymentTimelineRepository = paymentTimelineRepository;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry != null ? meterRegistry : new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        this.paymentsInitiatedCounter = io.micrometer.core.instrument.Counter.builder("payments_initiated_total")
                .description("Total number of initiated payment transactions")
                .register(this.meterRegistry);
        this.paymentsSettledCounter = io.micrometer.core.instrument.Counter.builder("payments_settled_total")
                .description("Total number of successfully settled payments")
                .register(this.meterRegistry);
        this.settlementTimer = io.micrometer.core.instrument.Timer.builder("payment_settlement_duration_seconds")
                .description("Latency distribution for payment settlement")
                .register(this.meterRegistry);

        // Outbox pending events gauge
        io.micrometer.core.instrument.Gauge.builder("outbox_pending_events", () -> (double) this.outboxRepository.countByStatus("PENDING"))
                .description("Number of pending events in transactional outbox waiting to be published")
                .register(this.meterRegistry);
    }

    public PaymentService(PaymentRepository paymentRepository,
                          OutboxRepository outboxRepository,
                          PaymentTimelineRepository paymentTimelineRepository,
                          ObjectMapper objectMapper) {
        this(paymentRepository, outboxRepository, paymentTimelineRepository, objectMapper, new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
    }

    // FR-5 & FR-21: Initiate payment with Idempotency Key and RBAC checks
    @Transactional
    public InitiationResult initiatePayment(String idempotencyKey, InitiatePaymentRequest request, UUID requestingUserId, String role) {
        if ("ADMIN".equalsIgnoreCase(role)) {
            // Invariant FR-21: Admin cannot spend a user's money (no impersonated transfers)
            throw new ForbiddenException("Administrators are prohibited from initiating transfers from user accounts. Impersonated spending is strictly forbidden.");
        }

        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }
        if (request.senderAccountId().equals(request.receiverAccountId())) {
            throw new IllegalArgumentException("Sender and receiver account cannot be the same");
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be strictly greater than zero");
        }

        Optional<Payment> existingOpt = paymentRepository.findByIdempotencyKey(idempotencyKey.trim());
        if (existingOpt.isPresent()) {
            Payment existing = existingOpt.get();
            // Check for payload match
            boolean sameSender = existing.getSenderAccountId().equals(request.senderAccountId());
            boolean sameReceiver = existing.getReceiverAccountId().equals(request.receiverAccountId());
            boolean sameAmount = existing.getAmount().compareTo(request.amount()) == 0;

            if (sameSender && sameReceiver && sameAmount) {
                // Exact same payload: Return existing payment with 200 OK
                return new InitiationResult(PaymentResponse.from(existing, getTimelineInternal(existing.getId())), false);
            } else {
                // Same key with different payload: 409 Conflict
                throw new IdempotencyPayloadMismatchException(idempotencyKey.trim());
            }
        }

        // New Payment aggregate creation with initiating userId
        Payment payment = new Payment(
                idempotencyKey.trim(),
                request.senderAccountId(),
                request.receiverAccountId(),
                request.amount(),
                request.currency(),
                requestingUserId
        );
        Payment savedPayment = paymentRepository.save(payment);
        paymentsInitiatedCounter.increment();

        // Record initial timeline entry
        paymentTimelineRepository.save(new PaymentTimeline(
                savedPayment.getId(),
                PaymentStatus.INITIATED,
                savedPayment.getCreatedAt(),
                null
        ));

        // Outbox Pattern: Save event in same DB transaction
        PaymentInitiatedEvent event = new PaymentInitiatedEvent(
                savedPayment.getId(),
                savedPayment.getIdempotencyKey(),
                savedPayment.getSenderAccountId(),
                savedPayment.getReceiverAccountId(),
                savedPayment.getAmount(),
                savedPayment.getCurrency(),
                Instant.now()
        );

        try {
            String payloadJson = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = new OutboxEvent(
                    "PAYMENT",
                    savedPayment.getId(),
                    "PaymentInitiated",
                    payloadJson
            );
            outboxRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox event payload", e);
        }

        return new InitiationResult(PaymentResponse.from(savedPayment, getTimelineInternal(savedPayment.getId())), true);
    }

    public InitiationResult initiatePayment(String idempotencyKey, InitiatePaymentRequest request) {
        return initiatePayment(idempotencyKey, request, null, null);
    }

    // FR-7 & FR-21: Query payment by ID with RBAC check
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID id, UUID requestingUserId, String role) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
        validatePaymentAccess(payment, requestingUserId, role);
        return PaymentResponse.from(payment, getTimelineInternal(payment.getId()));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID id) {
        return getPayment(id, null, null);
    }

    // FR-21: List all payments with RBAC (Admin sees all; User sees only their own payments)
    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments(UUID requestingUserId, String role) {
        if ("ADMIN".equalsIgnoreCase(role) || requestingUserId == null) {
            return paymentRepository.findAll().stream()
                    .map(p -> PaymentResponse.from(p, getTimelineInternal(p.getId())))
                    .collect(Collectors.toList());
        }
        Specification<Payment> spec = (root, query, cb) -> cb.equal(root.get("userId"), requestingUserId);
        return paymentRepository.findAll(spec).stream()
                .map(p -> PaymentResponse.from(p, getTimelineInternal(p.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments() {
        return getAllPayments(null, null);
    }

    // Phase B: Timeline entries for a payment with RBAC check
    @Transactional(readOnly = true)
    public List<TimelineEntryResponse> getTimeline(UUID paymentId, UUID requestingUserId, String role) {
        getPayment(paymentId, requestingUserId, role);
        return getTimelineInternal(paymentId);
    }

    @Transactional(readOnly = true)
    public List<TimelineEntryResponse> getTimeline(UUID paymentId) {
        return getTimeline(paymentId, null, null);
    }

    private List<TimelineEntryResponse> getTimelineInternal(UUID paymentId) {
        return paymentTimelineRepository.findByPaymentIdOrderByTimestampAsc(paymentId)
                .stream()
                .map(TimelineEntryResponse::from)
                .toList();
    }

    // Phase B: Search, Filter, Pagination with RBAC
    @Transactional(readOnly = true)
    public Page<PaymentResponse> searchPayments(PaymentStatus status, Instant from, Instant to, String search, Pageable pageable, UUID requestingUserId, String role) {
        Specification<Payment> spec = Specification.where(null);

        if (!"ADMIN".equalsIgnoreCase(role) && requestingUserId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("userId"), requestingUserId));
        }

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }
        if (search != null && !search.trim().isEmpty()) {
            String term = search.trim();
            spec = spec.and((root, query, cb) -> {
                try {
                    UUID searchUuid = UUID.fromString(term);
                    return cb.or(
                            cb.equal(root.get("id"), searchUuid),
                            cb.like(cb.lower(root.get("idempotencyKey")), "%" + term.toLowerCase() + "%")
                    );
                } catch (IllegalArgumentException e) {
                    return cb.like(cb.lower(root.get("idempotencyKey")), "%" + term.toLowerCase() + "%");
                }
            });
        }

        return paymentRepository.findAll(spec, pageable)
                .map(p -> PaymentResponse.from(p, getTimelineInternal(p.getId())));
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> searchPayments(PaymentStatus status, Instant from, Instant to, String search, Pageable pageable) {
        return searchPayments(status, from, to, search, pageable, null, null);
    }

    // State machine status updates + timeline tracking
    @Transactional
    public PaymentResponse validatePayment(UUID id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
        payment.validate();
        Payment updated = paymentRepository.save(payment);
        paymentTimelineRepository.save(new PaymentTimeline(id, PaymentStatus.VALIDATED, Instant.now(), null));
        return PaymentResponse.from(updated, getTimelineInternal(id));
    }

    @Transactional
    public PaymentResponse settlePayment(UUID id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
        payment.settle();
        Payment updated = paymentRepository.save(payment);
        paymentTimelineRepository.save(new PaymentTimeline(id, PaymentStatus.SETTLED, Instant.now(), null));
        paymentsSettledCounter.increment();
        if (payment.getCreatedAt() != null) {
            settlementTimer.record(java.time.Duration.between(payment.getCreatedAt(), Instant.now()));
        }
        return PaymentResponse.from(updated, getTimelineInternal(id));
    }

    @Transactional
    public PaymentResponse failPayment(UUID id, String reason) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
        payment.fail(reason);
        Payment updated = paymentRepository.save(payment);
        paymentTimelineRepository.save(new PaymentTimeline(id, PaymentStatus.FAILED, Instant.now(), reason));
        String failureTag = (reason != null && !reason.isBlank()) ? reason : "UNKNOWN";
        meterRegistry.counter("payments_failed_total", "reason", failureTag).increment();
        return PaymentResponse.from(updated, getTimelineInternal(id));
    }

    private void validatePaymentAccess(Payment payment, UUID requestingUserId, String role) {
        if ("ADMIN".equalsIgnoreCase(role) || requestingUserId == null) {
            return;
        }
        if (payment.getUserId() != null && !payment.getUserId().equals(requestingUserId)) {
            throw new ForbiddenException("Access denied: You do not have permission to view this payment");
        }
    }
}
