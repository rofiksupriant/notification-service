package com.vibe.notification.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Async configuration for @Async processing
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // Spring will use the default ThreadPoolTaskExecutor from application.yml
    // Properties: spring.task.execution.pool.*
}
