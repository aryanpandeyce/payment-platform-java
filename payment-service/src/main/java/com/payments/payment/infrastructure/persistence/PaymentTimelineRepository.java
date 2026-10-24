package com.payments.payment.infrastructure.persistence;

import com.payments.payment.domain.PaymentTimeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentTimelineRepository extends JpaRepository<PaymentTimeline, UUID> {
    List<PaymentTimeline> findByPaymentIdOrderByTimestampAsc(UUID paymentId);
}
