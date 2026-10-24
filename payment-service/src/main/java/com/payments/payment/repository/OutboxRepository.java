package com.payments.payment.repository;

import com.payments.payment.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

// @Repository: Outbox events repository
@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    // Background poller ke liye pending events fetch karna in order
    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(String status);
}
