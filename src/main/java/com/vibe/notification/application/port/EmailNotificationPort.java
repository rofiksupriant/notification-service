package com.vibe.notification.application.port;

import com.vibe.notification.domain.dto.TemplateDTO;

/**
 * Port for sending email notifications.
 * Application layer defines the contract; infrastructure adapts to it.
 */
public interface EmailNotificationPort {
    void sendEmail(String recipient, TemplateDTO template, String renderedSubject, String renderedContent);
}
