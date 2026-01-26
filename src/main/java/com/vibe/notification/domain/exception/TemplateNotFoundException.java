package com.vibe.notification.domain.exception;

public class TemplateNotFoundException extends NotificationException {
    public TemplateNotFoundException(String slug, String language) {
        super(String.format("Template not found for slug='%s' and language='%s'", slug, language));
    }
}
