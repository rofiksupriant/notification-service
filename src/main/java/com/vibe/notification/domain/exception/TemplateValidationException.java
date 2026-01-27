package com.vibe.notification.domain.exception;

/**
 * Exception thrown when template validation fails
 */
public class TemplateValidationException extends NotificationException {
    public TemplateValidationException(String message) {
        super(message);
    }

    public TemplateValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
