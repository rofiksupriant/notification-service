package com.vibe.notification.application.dto;

import com.vibe.notification.domain.model.Channel;
import java.time.LocalDateTime;

/**
 * DTO for template response in API responses
 */
public record TemplateResponse(
    String slug,
    String language,
    Channel channel,
    String templateType,
    String subject,
    String content,
    String imageUrl,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
