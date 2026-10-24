package com.payments.payment.repository;

import com.payments.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

// @Repository: Spring Data JPA repository for Payment entity
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    // Idempotency check ke liye key se existing payment search karna
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
