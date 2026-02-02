package com.vibe.notification.infrastructure.config.observability;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * EnvironmentPostProcessor that programmatically excludes OpenTelemetry auto-configuration
 * when the OTel feature is disabled via app.feature.otel.enabled=false.
 * 
 * This runs early in the Spring Boot lifecycle, before auto-configuration classes are processed,
 * ensuring that OpenTelemetry auto-configuration is completely disabled when the feature toggle
 * is set to false.
 */
public class OpenTelemetryAutoConfigurationExcluder implements EnvironmentPostProcessor {

    private static final String OTEL_FEATURE_PROPERTY = "app.feature.otel.enabled";
    private static final String AUTOCONFIGURE_EXCLUDE_PROPERTY = "spring.autoconfigure.exclude";
    private static final String OTEL_AUTO_CONFIG_CLASS = 
            "io.opentelemetry.instrumentation.spring.autoconfigure.OpenTelemetryAutoConfiguration";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // Check if OTel feature is explicitly disabled
        String otelEnabled = environment.getProperty(OTEL_FEATURE_PROPERTY);
        
        if ("false".equalsIgnoreCase(otelEnabled)) {
            // Get existing exclusions
            String existingExclusions = environment.getProperty(AUTOCONFIGURE_EXCLUDE_PROPERTY, "");
            
            // Add OpenTelemetry auto-configuration to exclusions if not already present
            String updatedExclusions;
            if (existingExclusions.isEmpty()) {
                updatedExclusions = OTEL_AUTO_CONFIG_CLASS;
            } else if (!existingExclusions.contains(OTEL_AUTO_CONFIG_CLASS)) {
                updatedExclusions = existingExclusions + "," + OTEL_AUTO_CONFIG_CLASS;
            } else {
                // Already excluded, nothing to do
                return;
            }
            
            // Add the updated exclusions to the environment
            Map<String, Object> properties = new HashMap<>();
            properties.put(AUTOCONFIGURE_EXCLUDE_PROPERTY, updatedExclusions);
            
            environment.getPropertySources().addFirst(
                new MapPropertySource("otelAutoConfigExcluder", properties)
            );
        }
    }
}
