package com.vibe.notification.test.support;

import com.vibe.notification.domain.dto.TemplateDTO;
import com.vibe.notification.domain.model.Channel;
import com.vibe.notification.domain.dto.TemplateIdDTO;
import java.time.LocalDateTime;

/**
 * Testable factory for creating DTOs in tests.
 * This can be used by integration tests to set up test data without creating infrastructure entities.
 */
public class TemplateTestDataFactory {
    
    public static TemplateDTO createTemplateDTO(
            String slug, String language, Channel channel,
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

    public static TemplateDTO createTemplateDTO(String slug, String language, Channel channel, String subject, String content) {
        return createTemplateDTO(slug, language, channel, subject, content, null);
    }
}
