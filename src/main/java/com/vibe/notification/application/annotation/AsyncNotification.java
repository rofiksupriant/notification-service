package com.vibe.notification.application.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation to mark methods for asynchronous notification processing.
 * Methods annotated with @AsyncNotification will be intercepted by NotificationProcessingAspect
 * and executed asynchronously with proper error handling and MDC context management.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AsyncNotification {
}
