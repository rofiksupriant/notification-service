package com.vibe.notification.application.port;

import com.vibe.notification.domain.dto.TemplateDTO;

/**
 * Port for sending WhatsApp notifications.
 * Application layer defines the contract; infrastructure adapts to it.
 */
public interface WhatsAppNotificationPort {
    void sendWhatsAppMessage(String recipient, TemplateDTO template, String renderedContent);
}
