package com.vibe.notification.domain.service;

import com.vibe.notification.domain.exception.TemplateNotFoundException;
import com.vibe.notification.infrastructure.persistence.entity.NotificationTemplateEntity;
import com.vibe.notification.infrastructure.persistence.entity.NotificationTemplateId;
import com.vibe.notification.infrastructure.persistence.repository.NotificationTemplateRepository;
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

    private final NotificationTemplateRepository templateRepository;

    public TemplateResolutionService(NotificationTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    /**
     * Resolve template by slug and language with fallback logic
     * Fallback order: 1. requested language, 2. default 'en'
     */
    public NotificationTemplateEntity resolveTemplate(String slug, String language) {
        logger.debug("Resolving template: slug={}, language={}", slug, language);

        // Try requested language first
        var templateId = new NotificationTemplateId(slug, language);
        var template = templateRepository.findById(templateId);
        
        if (template.isPresent()) {
            logger.debug("Template found for requested language: {}", language);
            return template.get();
        }

        logger.info("Template not found for language '{}', attempting fallback to '{}'", language, DEFAULT_LANGUAGE);

        // Fallback to default language if different from requested
        if (!language.equals(DEFAULT_LANGUAGE)) {
            var fallbackId = new NotificationTemplateId(slug, DEFAULT_LANGUAGE);
            var fallbackTemplate = templateRepository.findById(fallbackId);
            
            if (fallbackTemplate.isPresent()) {
                logger.debug("Template found with fallback language: {}", DEFAULT_LANGUAGE);
                return fallbackTemplate.get();
            }
        }

        // Not found in either language
        logger.error("Template resolution failed: slug={}, attempted languages=[{}, {}]", slug, language, DEFAULT_LANGUAGE);
        throw new TemplateNotFoundException(slug, language);
    }
}
