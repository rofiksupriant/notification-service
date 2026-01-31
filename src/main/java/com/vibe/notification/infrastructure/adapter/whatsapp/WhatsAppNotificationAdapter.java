package com.vibe.notification.infrastructure.adapter.whatsapp;

import com.vibe.notification.application.port.WhatsAppNotificationPort;
import com.vibe.notification.domain.dto.TemplateDTO;
import com.vibe.notification.domain.exception.NotificationException;
import com.vibe.notification.domain.model.TemplateType;
import com.vibe.notification.infrastructure.external.watzap.WatzapClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * WhatsApp notification adapter implementing WhatsAppNotificationPort
 * Supports both text and image messages
 */
@Component
public class WhatsAppNotificationAdapter implements WhatsAppNotificationPort {
    private static final Logger logger = LoggerFactory.getLogger(WhatsAppNotificationAdapter.class);

    private final WatzapClient watzapClient;

    public WhatsAppNotificationAdapter(WatzapClient watzapClient) {
        this.watzapClient = watzapClient;
    }

    /**
     * Send WhatsApp notification
     */
    @Override
    public void sendWhatsAppMessage(String phoneNumber, TemplateDTO template, String renderedContent) {
        try {
            logger.debug("Sending WhatsApp message to: {}", phoneNumber);

            var templateType = TemplateType.from(template.getType());

            switch (templateType) {
              case TemplateType.TEXT -> sendTextMessage(phoneNumber, renderedContent);
              case TemplateType.IMAGE -> sendImageMessage(phoneNumber, template.getImageUrl(), renderedContent);
              default -> throw new NotificationException("Unsupported template type: " + templateType);
            }

            logger.info("WhatsApp message sent successfully to: {}", phoneNumber);
        } catch (Exception e) {
            logger.error("Failed to send WhatsApp message to {}: {}", phoneNumber, e.getMessage());
            throw new NotificationException("Failed to send WhatsApp message: " + e.getMessage(), e);
        }
    }

    private void sendTextMessage(String phoneNumber, String message) {
        var response = watzapClient.sendTextMessage(phoneNumber, message);
        if (!response.isSuccess()) {
            throw new NotificationException("Failed to send text message: " + response.message());
        }
    }

    private void sendImageMessage(String phoneNumber, String imageUrl, String caption) {
        var response = watzapClient.sendImageMessage(phoneNumber, imageUrl, caption);
        if (!response.isSuccess()) {
            throw new NotificationException("Failed to send image message: " + response.message());
        }
    }
}
