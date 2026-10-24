package com.payments.notification.repository;

import com.payments.notification.domain.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// @Repository: Spring Data MongoDB repository (append-only ledger audit trail)
@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

    List<AuditLog> findByAggregateIdOrderByTimestampAsc(String aggregateId);
}
