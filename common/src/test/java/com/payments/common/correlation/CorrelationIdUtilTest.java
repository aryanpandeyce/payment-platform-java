package com.payments.common.correlation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationIdUtilTest {

    @AfterEach
    void tearDown() {
        CorrelationIdUtil.clearMdc();
    }

    @Test
    void shouldReturnExistingHeaderIfPresent() {
        String existing = "my-test-correlation-id";
        String result = CorrelationIdUtil.getOrGenerate(existing);
        assertEquals(existing, result);
    }

    @Test
    void shouldGenerateNewUuidIfHeaderNullOrBlank() {
        String resultNull = CorrelationIdUtil.getOrGenerate(null);
        assertNotNull(resultNull);
        assertFalse(resultNull.isBlank());

        String resultBlank = CorrelationIdUtil.getOrGenerate("   ");
        assertNotNull(resultBlank);
        assertFalse(resultBlank.isBlank());
    }

    @Test
    void shouldBindAndClearMdcContext() {
        CorrelationIdUtil.setInMdc("test-corr-123");
        assertEquals("test-corr-123", CorrelationIdUtil.getFromMdc());

        CorrelationIdUtil.clearMdc();
        assertNull(CorrelationIdUtil.getFromMdc());
    }
}
