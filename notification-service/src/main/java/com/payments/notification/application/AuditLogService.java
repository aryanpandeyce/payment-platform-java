package com.payments.notification.application;

import com.payments.notification.api.dto.AuditLogResponse;
import com.payments.notification.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class AuditLogService {

    private final MongoTemplate mongoTemplate;

    public AuditLogService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Page<AuditLogResponse> searchAuditLogs(String eventType,
                                                  String correlationId,
                                                  String aggregateId,
                                                  Instant from,
                                                  Instant to,
                                                  Pageable pageable) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        if (eventType != null && !eventType.trim().isEmpty()) {
            criteriaList.add(Criteria.where("eventType").is(eventType.trim()));
        }
        if (correlationId != null && !correlationId.trim().isEmpty()) {
            criteriaList.add(Criteria.where("correlationId").is(correlationId.trim()));
        }
        if (aggregateId != null && !aggregateId.trim().isEmpty()) {
            criteriaList.add(Criteria.where("aggregateId").is(aggregateId.trim()));
        }
        if (from != null && to != null) {
            criteriaList.add(Criteria.where("timestamp").gte(from).lte(to));
        } else if (from != null) {
            criteriaList.add(Criteria.where("timestamp").gte(from));
        } else if (to != null) {
            criteriaList.add(Criteria.where("timestamp").lte(to));
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        long total = mongoTemplate.count(query, AuditLog.class);
        query.with(pageable);
        List<AuditLog> logs = mongoTemplate.find(query, AuditLog.class);

        List<AuditLogResponse> responses = logs.stream()
                .map(AuditLogResponse::from)
                .toList();

        return new PageImpl<>(responses, pageable, total);
    }
}
