package com.payments.account.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.account.domain.AccountNotFoundException;
import com.payments.account.domain.InsufficientFundsException;
import com.payments.account.event.PaymentFailedEvent;
import com.payments.account.event.PaymentSettledEvent;
import com.payments.account.event.PaymentValidatedEvent;
import com.payments.account.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// @Component: Kafka listener jo payment-validated events consume karta hai aur atomic settlement execute karta hai
@Component
public class SettlementEventListener {

    private static final Logger log = LoggerFactory.getLogger(SettlementEventListener.class);
    private static final String TOPIC_SETTLED = "payment-settled";
    private static final String TOPIC_FAILED = "payment-failed";

    private final AccountService accountService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // Idempotent consumer: duplicate events ignore karne ke liye cache
    private final Set<UUID> processedPayments = ConcurrentHashMap.newKeySet();

    public SettlementEventListener(AccountService accountService,
                                   KafkaTemplate<String, String> kafkaTemplate,
                                   ObjectMapper objectMapper) {
        this.accountService = accountService;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "payment-validated", groupId = "account-service-group")
    public void onPaymentValidated(String message) {
        try {
            PaymentValidatedEvent event = objectMapper.readValue(message, PaymentValidatedEvent.class);

            // Idempotency check: agar already settle ho chuka hai toh skip karo
            if (!processedPayments.add(event.getPaymentId())) {
                log.info("Duplicate payment-validated event ignored for paymentId: {}", event.getPaymentId());
                return;
            }

            log.info("Executing settlement for paymentId: {}, sender: {}, receiver: {}, amount: {}",
                    event.getPaymentId(), event.getSenderAccountId(), event.getReceiverAccountId(), event.getAmount());

            try {
                // FR-10: Atomic transfer in ONE database transaction
                accountService.transfer(event.getSenderAccountId(), event.getReceiverAccountId(), event.getAmount());

                // Settlement successful
                PaymentSettledEvent settledEvent = new PaymentSettledEvent(
                        event.getPaymentId(),
                        event.getSenderAccountId(),
                        event.getReceiverAccountId(),
                        event.getAmount(),
                        event.getCurrency()
                );
                String payload = objectMapper.writeValueAsString(settledEvent);
                kafkaTemplate.send(TOPIC_SETTLED, event.getPaymentId().toString(), payload);
                log.info("Settlement SUCCESS. Published to '{}' for paymentId: {}", TOPIC_SETTLED, event.getPaymentId());

            } catch (InsufficientFundsException e) {
                // Balance kam tha
                log.warn("Settlement FAILED for paymentId: {} due to insufficient funds", event.getPaymentId());
                publishFailure(event.getPaymentId(), "INSUFFICIENT_FUNDS");

            } catch (AccountNotFoundException e) {
                // FR-11: Compensation logic (receiver account na milne par transaction rollback ho gaya aur refund record hua)
                log.warn("Settlement FAILED for paymentId: {} due to missing account: {}", event.getPaymentId(), e.getMessage());
                publishFailure(event.getPaymentId(), "RECEIVER_CREDIT_FAILED_REFUNDED");

            } catch (Exception e) {
                log.error("Settlement FAILED for paymentId: {} with unexpected error: {}", event.getPaymentId(), e.getMessage());
                publishFailure(event.getPaymentId(), "SETTLEMENT_FAILED");
            }

        } catch (Exception e) {
            log.error("Error deserializing payment-validated event: {}", e.getMessage(), e);
        }
    }

    private void publishFailure(UUID paymentId, String reason) {
        try {
            PaymentFailedEvent failedEvent = new PaymentFailedEvent(paymentId, reason);
            String payload = objectMapper.writeValueAsString(failedEvent);
            kafkaTemplate.send(TOPIC_FAILED, paymentId.toString(), payload);
            log.info("Published settlement failure to '{}' for paymentId: {}", TOPIC_FAILED, paymentId);
        } catch (Exception ex) {
            log.error("Failed to publish settlement failure event for paymentId: {}", paymentId, ex);
        }
    }
}
