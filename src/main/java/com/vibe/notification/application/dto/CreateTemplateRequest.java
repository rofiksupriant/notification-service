package com.vibe.notification.application.dto;

import com.vibe.notification.domain.model.Channel;

/**
 * DTO for creating a new notification template
 */
public record CreateTemplateRequest(
    String slug,
    String language,
    Channel channel,
    String content,
    String subject,
    String imageUrl,
    String templateType
) {}
