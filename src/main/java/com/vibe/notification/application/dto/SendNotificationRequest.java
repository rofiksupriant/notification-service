package com.vibe.notification.application.dto;

import java.util.Map;

/**
 * Send Notification Request DTO
 */
public record SendNotificationRequest(
    String recipient,
    String slug,
    String language,
    String channel,
    Map<String, Object> variables
) {}
