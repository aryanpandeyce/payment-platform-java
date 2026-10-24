package com.payments.common.correlation;

import org.slf4j.MDC;
import java.util.UUID;

/**
 * Utility for distributed correlation ID handling and MDC logging.
 */
public final class CorrelationIdUtil {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";

    private CorrelationIdUtil() {}

    /**
     * Returns the existing correlation ID if valid, or generates a new random UUID.
     */
    public static String getOrGenerate(String headerValue) {
        if (headerValue != null && !headerValue.trim().isEmpty()) {
            return headerValue.trim();
        }
        return UUID.randomUUID().toString();
    }

    /**
     * Binds the correlation ID to the SLF4J MDC context for structured logging.
     */
    public static void setInMdc(String correlationId) {
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
    }

    /**
     * Retrieves the correlation ID from the active thread MDC context.
     */
    public static String getFromMdc() {
        return MDC.get(CORRELATION_ID_MDC_KEY);
    }

    /**
     * Clears the correlation ID from the active thread MDC context.
     */
    public static void clearMdc() {
        MDC.remove(CORRELATION_ID_MDC_KEY);
    }
}
