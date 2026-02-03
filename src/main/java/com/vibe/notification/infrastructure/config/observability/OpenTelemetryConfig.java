package com.vibe.notification.infrastructure.config.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenTelemetry configuration for the notification service.
 * Configures traces, metrics, and logs export to OpenTelemetry Collector.
 * 
 * The OpenTelemetry instance is auto-configured by Spring Boot through the
 * opentelemetry-spring-boot-starter dependency. This configuration provides
 * additional beans for manual instrumentation if needed.
 * 
 * This configuration is only active when app.feature.otel.enabled=true
 */
@Configuration
@ConditionalOnProperty(name = "app.feature.otel.enabled", havingValue = "true", matchIfMissing = true)
public class OpenTelemetryConfig {

    private static final Logger log = LoggerFactory.getLogger(OpenTelemetryConfig.class);

    @Value("${otel.exporter.otlp.endpoint:http://localhost:4318}")
    private String otlpEndpoint;

    @Value("${otel.resource.attributes.service.name:notification-service}")
    private String serviceName;

    @Value("${otel.resource.attributes.service.version:1.0.0}")
    private String serviceVersion;

    @Value("${otel.resource.attributes.deployment.environment:development}")
    private String environment;

    /**
     * Provides a Tracer for manual instrumentation if needed.
     * The OpenTelemetry instance is auto-configured by Spring Boot.
     */
    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        log.info("Initializing OpenTelemetry Tracer with endpoint: {}, service: {}, version: {}, environment: {}",
                otlpEndpoint, serviceName, serviceVersion, environment);
        return openTelemetry.getTracer(serviceName, serviceVersion);
    }
}
