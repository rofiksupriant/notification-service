package com.vibe.notification.infrastructure.config.observability;

import java.util.Map;

/**
 * Record representing telemetry metadata for observability.
 * Used to capture and pass observability attributes across components.
 */
public record TelemetryMetadata(
        String serviceName,
        String serviceVersion,
        String environment,
        Map<String, String> customAttributes
) {
    /**
     * Creates TelemetryMetadata with minimal required fields.
     */
    public static TelemetryMetadata of(String serviceName, String serviceVersion, String environment) {
        return new TelemetryMetadata(serviceName, serviceVersion, environment, Map.of());
    }

    /**
     * Creates TelemetryMetadata with custom attributes.
     */
    public static TelemetryMetadata withAttributes(
            String serviceName, 
            String serviceVersion, 
            String environment,
            Map<String, String> customAttributes) {
        return new TelemetryMetadata(serviceName, serviceVersion, environment, Map.copyOf(customAttributes));
    }

    /**
     * Merges this metadata with additional attributes.
     */
    public TelemetryMetadata withAdditionalAttributes(Map<String, String> additionalAttributes) {
        Map<String, String> merged = new java.util.HashMap<>(customAttributes);
        merged.putAll(additionalAttributes);
        return new TelemetryMetadata(serviceName, serviceVersion, environment, Map.copyOf(merged));
    }
}
