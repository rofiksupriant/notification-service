package com.vibe.notification.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for managing trace_id in MDC (Mapped Diagnostic Context)
 */
@Service
public class TraceService {
    private static final Logger logger = LoggerFactory.getLogger(TraceService.class);
    private static final String TRACE_ID_KEY = "traceId";

    /**
     * Generate or retrieve trace_id and set it in MDC
     */
    public UUID setTraceId(UUID traceId) {
        if (traceId == null) {
            traceId = UUID.randomUUID();
        }
        MDC.put(TRACE_ID_KEY, traceId.toString());
        logger.debug("Trace ID set: {}", traceId);
        return traceId;
    }

    /**
     * Get current trace_id from MDC
     */
    public String getTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }

    /**
     * Clear trace_id from MDC
     */
    public void clearTraceId() {
        MDC.remove(TRACE_ID_KEY);
    }

    /**
     * Generate new trace_id
     */
    public UUID generateTraceId() {
        return setTraceId(UUID.randomUUID());
    }
}
