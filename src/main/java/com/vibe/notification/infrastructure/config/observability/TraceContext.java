package com.vibe.notification.infrastructure.config.observability;

import java.time.Instant;

/**
 * Record representing OpenTelemetry trace context information.
 * Used for passing trace metadata across async boundaries.
 */
public record TraceContext(
        String traceId,
        String spanId,
        String parentSpanId,
        boolean sampled,
        Instant timestamp
) {
    /**
     * Creates a TraceContext with current timestamp.
     */
    public static TraceContext of(String traceId, String spanId, String parentSpanId, boolean sampled) {
        return new TraceContext(traceId, spanId, parentSpanId, sampled, Instant.now());
    }

    /**
     * Creates a TraceContext without parent span.
     */
    public static TraceContext root(String traceId, String spanId, boolean sampled) {
        return new TraceContext(traceId, spanId, null, sampled, Instant.now());
    }
}
