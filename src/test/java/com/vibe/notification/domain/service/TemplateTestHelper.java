package com.vibe.notification.domain.service;

import com.vibe.notification.domain.dto.TemplateDTO;
import com.vibe.notification.domain.dto.TemplateIdDTO;
import java.time.LocalDateTime;

/**
 * Helper class for creating test DTOs within the domain test package.
 * This isolates DTO creation from infrastructure entity dependencies.
 */
public class TemplateTestHelper {
    
    public static TemplateDTO createTemplateDTO(
            String slug, String language, String channel,
            String subject, String content, String imageUrl) {
        LocalDateTime now = LocalDateTime.now();
        return new TemplateDTO(
            new TemplateIdDTO(slug, language, channel),
            slug,
            language,
            channel,
            "TEXT",
            subject,
            content,
            imageUrl,
            now,
            now
        );
    }

    public static TemplateDTO createTemplateDTO(String slug, String language, String channel, String subject, String content) {
        return createTemplateDTO(slug, language, channel, subject, content, null);
    }
}

