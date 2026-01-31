package com.vibe.notification.domain.exception;

public class TemplateRenderingException extends NotificationException {
    private static final long serialVersionUID = 1L;

    public TemplateRenderingException(String message, Throwable cause) {
        super(message, cause);
    }
}
