package com.vibe.notification.domain.service;

import com.vibe.notification.domain.dto.TemplateDTO;
import com.vibe.notification.domain.dto.TemplateIdDTO;
import com.vibe.notification.domain.exception.TemplateNotFoundException;
import com.vibe.notification.domain.port.NotificationTemplatePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for template resolution with language fallback logic
 * Fallback chain: requested language -> default 'en'
 */
@Service
public class TemplateResolutionService {
    private static final Logger logger = LoggerFactory.getLogger(TemplateResolutionService.class);
    private static final String DEFAULT_LANGUAGE = "en";

    private final NotificationTemplatePort templatePort;

    public TemplateResolutionService(NotificationTemplatePort templatePort) {
        this.templatePort = templatePort;
    }

    /**
     * Resolve template by slug, language, and channel with fallback logic
     * Fallback order: 1. requested language, 2. default 'en'
     */
    public TemplateDTO resolveTemplate(String slug, String language, String channel) {
        logger.debug("Resolving template: slug={}, language={}, channel={}", slug, language, channel);

        // Try requested language first
        var templateId = new TemplateIdDTO(slug, language, channel);
        var template = templatePort.findById(templateId);
        
        if (template.isPresent()) {
            logger.debug("Template found for requested language: {}", language);
            return template.get();
        }

        logger.info("Template not found for language '{}', attempting fallback to '{}'", language, DEFAULT_LANGUAGE);

        // Fallback to default language if different from requested
        if (!language.equals(DEFAULT_LANGUAGE)) {
            var fallbackId = new TemplateIdDTO(slug, DEFAULT_LANGUAGE, channel);
            var fallbackTemplate = templatePort.findById(fallbackId);
            
            if (fallbackTemplate.isPresent()) {
                logger.debug("Template found with fallback language: {}", DEFAULT_LANGUAGE);
                return fallbackTemplate.get();
            }
        }

        // Not found in either language
        logger.error("Template resolution failed: slug={}, channel={}, attempted languages=[{}, {}]", slug, channel, language, DEFAULT_LANGUAGE);
        throw new TemplateNotFoundException(slug, language);
    }
}
