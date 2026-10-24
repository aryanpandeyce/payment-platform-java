package com.payments.payment.infrastructure.persistence;

import com.payments.payment.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(String status);
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(String status);
}
