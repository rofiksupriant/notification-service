package com.vibe.notification.application.dto;

/**
 * DTO for updating an existing notification template
 */
public record UpdateTemplateRequest(
    String content,
    String subject,
    String imageUrl
) {}
