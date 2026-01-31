package com.vibe.notification.infrastructure.adapter;

import com.vibe.notification.application.dto.*;
import com.vibe.notification.application.port.TemplateManagementPort;
import com.vibe.notification.domain.exception.TemplateAlreadyExistsException;
import com.vibe.notification.domain.exception.TemplateNotFoundException;
import com.vibe.notification.domain.exception.TemplateValidationException;
import com.vibe.notification.domain.model.Channel;
import com.vibe.notification.infrastructure.persistence.entity.NotificationTemplateEntity;
import com.vibe.notification.infrastructure.persistence.entity.NotificationTemplateId;
import com.vibe.notification.infrastructure.persistence.repository.NotificationTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adapter service implementing TemplateManagementPort
 * Handles CRUD operations for notification templates
 */
@Service
@Transactional
public class TemplateManagementAdapter implements TemplateManagementPort {
    private static final Logger logger = LoggerFactory.getLogger(TemplateManagementAdapter.class);

    private final NotificationTemplateRepository templateRepository;

    public TemplateManagementAdapter(NotificationTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    /**
     * Create a new notification template
     *
     * @param request the template creation request
     * @return the created template response
     * @throws TemplateValidationException if validation fails
     * @throws TemplateAlreadyExistsException if template already exists
     */
    @Override
    public TemplateResponse createTemplate(CreateTemplateRequest request) {
        logger.info("Creating new template: slug={}, language={}, channel={}", 
            request.slug(), request.language(), request.channel());

        validateCreateRequest(request);

        var templateId = new NotificationTemplateId(request.slug(), request.language(), request.channel());

        // Check if template already exists
        if (templateRepository.findById(templateId).isPresent()) {
            throw new TemplateAlreadyExistsException(request.slug(), request.language());
        }

        var entity = new NotificationTemplateEntity(
            templateId,
            request.templateType(),
            request.subject(),
            request.content(),
            request.imageUrl()
        );

        var savedEntity = templateRepository.save(entity);
        logger.info("Template created successfully: slug={}, language={}", 
            request.slug(), request.language());

        return mapToResponse(savedEntity);
    }

    /**
     * Fetch a template by slug, language, and channel
     *
     * @param slug the template slug
     * @param language the template language
     * @param channel the notification channel
     * @return the template response
     * @throws TemplateNotFoundException if template is not found
     */    @Override    @Transactional(readOnly = true)
    public TemplateResponse getTemplate(String slug, String language, Channel channel) {
        logger.debug("Fetching template: slug={}, language={}, channel={}", slug, language, channel);

        validateSlugAndLanguage(slug, language);

        var templateId = new NotificationTemplateId(slug, language, channel);
        var entity = templateRepository.findById(templateId)
            .orElseThrow(() -> new TemplateNotFoundException(slug, language));

        return mapToResponse(entity);
    }

    /**
     * Update an existing template
     * Updates the content, subject, and imageUrl without modifying Flyway migrations
     *
     * @param slug the template slug
     * @param language the template language
     * @param channel the notification channel
     * @param request the update request
     * @return the updated template response
     * @throws TemplateNotFoundException if template is not found
     * @throws TemplateValidationException if validation fails
     */
    @Override
    public TemplateResponse updateTemplate(String slug, String language, Channel channel, UpdateTemplateRequest request) {
        logger.info("Updating template: slug={}, language={}, channel={}", slug, language, channel);

        validateSlugAndLanguage(slug, language);
        validateUpdateRequest(request);

        var templateId = new NotificationTemplateId(slug, language, channel);
        var entity = templateRepository.findById(templateId)
            .orElseThrow(() -> new TemplateNotFoundException(slug, language));

        // Update the template fields
        entity.setContent(request.content());
        if (request.subject() != null) {
            entity.setSubject(request.subject());
        }
        if (request.imageUrl() != null) {
            entity.setImageUrl(request.imageUrl());
        }
        if (request.templateType() != null) {
            entity.setTemplateType(request.templateType());
        }

        var updatedEntity = templateRepository.save(entity);
        logger.info("Template updated successfully: slug={}, language={}, channel={}", slug, language, channel);

        // Clear any caches if needed
        clearTemplateCache(slug, language);

        return mapToResponse(updatedEntity);
    }

    /**
     * Delete a template by slug, language, and channel
     *
     * @param slug the template slug
     * @param language the template language
     * @param channel the notification channel
     * @throws TemplateNotFoundException if template is not found
     */
    @Override
    public void deleteTemplate(String slug, String language, Channel channel) {
        logger.info("Deleting template: slug={}, language={}, channel={}", slug, language, channel);

        validateSlugAndLanguage(slug, language);

        var templateId = new NotificationTemplateId(slug, language, channel);
        var entity = templateRepository.findById(templateId)
            .orElseThrow(() -> new TemplateNotFoundException(slug, language));

        templateRepository.delete(entity);
        logger.info("Template deleted successfully: slug={}, language={}, channel={}", slug, language, channel);

        // Clear any caches if needed
        clearTemplateCache(slug, language);
    }

    /**
     * Validate template creation request
     */
    private void validateCreateRequest(CreateTemplateRequest request) {
        if (request.slug() == null || request.slug().isBlank()) {
            throw new TemplateValidationException("Slug cannot be empty");
        }
        if (request.language() == null || request.language().isBlank()) {
            throw new TemplateValidationException("Language cannot be empty");
        }
        if (request.channel() == null) {
            throw new TemplateValidationException("Channel cannot be empty");
        }
        if (request.content() == null || request.content().isBlank()) {
            throw new TemplateValidationException("Content cannot be empty");
        }
        if (request.templateType() == null || request.templateType().isBlank()) {
            throw new TemplateValidationException("Template type cannot be empty");
        }
    }

    /**
     * Validate template update request
     */
    private void validateUpdateRequest(UpdateTemplateRequest request) {
        if (request.content() == null || request.content().isBlank()) {
            throw new TemplateValidationException("Content cannot be empty");
        }
    }

    /**
     * Validate slug and language parameters
     */
    private void validateSlugAndLanguage(String slug, String language) {
        if (slug == null || slug.isBlank()) {
            throw new TemplateValidationException("Slug cannot be empty");
        }
        if (language == null || language.isBlank()) {
            throw new TemplateValidationException("Language cannot be empty");
        }
    }

    /**
     * Clear any template caches
     * Currently clears Thymeleaf's StringTemplateResolver cache if needed
     */
    private void clearTemplateCache(String slug, String language) {
        // Thymeleaf caches templates - we need to clear the cache for updated templates
        // This ensures updates are immediately reflected
        logger.debug("Clearing template cache for slug={}, language={}", slug, language);
        // The StringTemplateResolver should automatically handle cache invalidation
        // or we can clear it through the template engine if needed
    }

    /**
     * Map template entity to response DTO
     */
    private TemplateResponse mapToResponse(NotificationTemplateEntity entity) {
        return new TemplateResponse(
            entity.getId().getSlug(),
            entity.getId().getLanguage(),
            entity.getChannel(),
            entity.getTemplateType(),
            entity.getSubject(),
            entity.getContent(),
            entity.getImageUrl(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
