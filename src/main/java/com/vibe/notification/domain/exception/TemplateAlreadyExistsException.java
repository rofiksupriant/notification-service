package com.vibe.notification.domain.exception;

/**
 * Exception thrown when attempting to create a template that already exists
 */
public class TemplateAlreadyExistsException extends NotificationException {
    private static final long serialVersionUID = 1L;

    public TemplateAlreadyExistsException(String slug, String language) {
        super(String.format("Template already exists with slug '%s' and language '%s'", slug, language));
    }

    public TemplateAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
