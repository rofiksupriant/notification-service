package com.vibe.notification.infrastructure.config.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for OpenTelemetry trace propagation across async threads.
 * Ensures traceId and spanId are properly propagated to async executions.
 * 
 * This configuration is only active when app.feature.otel.enabled=true
 */
@Configuration
@ConditionalOnProperty(name = "app.feature.otel.enabled", havingValue = "true", matchIfMissing = true)
public class AsyncTracePropagationConfig implements AsyncConfigurer {

    private final OpenTelemetry openTelemetry;

    @Value("${otel.async.core-pool-size:5}")
    private int corePoolSize;

    @Value("${otel.async.max-pool-size:10}")
    private int maxPoolSize;

    @Value("${otel.async.queue-capacity:100}")
    private int queueCapacity;

    public AsyncTracePropagationConfig(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("async-otel-");
        executor.setTaskDecorator(new TraceContextTaskDecorator());
        executor.initialize();
        return executor;
    }

    /**
     * TaskDecorator that propagates OpenTelemetry context to async threads.
     * 
     * This is a static inner class because it doesn't require access to the outer class state.
     * The OpenTelemetry context is captured per-task from the calling thread using
     * Context.current(), not from the configuration instance. This design ensures that
     * each async task captures its own parent context at task submission time.
     */
    private static class TraceContextTaskDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            // Capture current context from the calling thread
            Context currentContext = Context.current();
            
            // Return wrapped runnable that restores context in the async thread
            return () -> {
                try (var ignored = currentContext.makeCurrent()) {
                    runnable.run();
                }
            };
        }
    }
}
