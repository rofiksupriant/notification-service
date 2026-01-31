package com.vibe.notification.domain.dto;

import java.time.LocalDateTime;

/**
 * Domain Data Transfer Object for notification templates
 * Used to transfer template data without exposing infrastructure entity details
 */
public class TemplateDTO {
    private final TemplateIdDTO id;
    private final String slug;
    private final String language;
    private final String channel;
    private final String type;
    private final String subject;
    private final String content;
    private final String imageUrl;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public TemplateDTO(
            TemplateIdDTO id,
            String slug,
            String language,
            String channel,
            String type,
            String subject,
            String content,
            String imageUrl,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = id;
        this.slug = slug;
        this.language = language;
        this.channel = channel;
        this.type = type;
        this.subject = subject;
        this.content = content;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public TemplateIdDTO getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getLanguage() {
        return language;
    }

    public String getChannel() {
        return channel;
    }

    public String getType() {
        return type;
    }

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
