package com.vibe.notification.domain.exception;

/**
 * Exception thrown when template validation fails
 */
public class TemplateValidationException extends NotificationException {
    private static final long serialVersionUID = 1L;

    public TemplateValidationException(String message) {
        super(message);
    }

    public TemplateValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
