package com.vibe.notification.application.dto;

/**
 * DTO for creating a new notification template
 */
public record CreateTemplateRequest(
    String slug,
    String language,
    String channel,
    String content,
    String subject,
    String imageUrl,
    String templateType
) {}
