package com.payments.notification;

import com.payments.notification.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class NotificationServiceApplicationTests {

    @MockBean
    private AuditLogRepository auditLogRepository;

    @Test
    void contextLoads() {
    }
}
