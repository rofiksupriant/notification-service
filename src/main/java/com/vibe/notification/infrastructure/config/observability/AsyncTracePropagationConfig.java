package com.vibe.notification.infrastructure.config.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for OpenTelemetry trace propagation across async threads.
 * Ensures traceId and spanId are properly propagated to async executions.
 */
@Configuration
public class AsyncTracePropagationConfig implements AsyncConfigurer {

    private final OpenTelemetry openTelemetry;

    public AsyncTracePropagationConfig(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-otel-");
        executor.setTaskDecorator(new TraceContextTaskDecorator());
        executor.initialize();
        return executor;
    }

    /**
     * TaskDecorator that propagates OpenTelemetry context to async threads.
     */
    private static class TraceContextTaskDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            // Capture current context
            Context currentContext = Context.current();
            
            // Return wrapped runnable that restores context
            return () -> {
                try (var ignored = currentContext.makeCurrent()) {
                    runnable.run();
                }
            };
        }
    }
}
