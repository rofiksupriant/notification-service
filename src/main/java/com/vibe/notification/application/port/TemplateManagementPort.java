package com.vibe.notification.application.port;

import com.vibe.notification.application.dto.CreateTemplateRequest;
import com.vibe.notification.application.dto.TemplateResponse;
import com.vibe.notification.application.dto.UpdateTemplateRequest;
import com.vibe.notification.domain.model.Channel;

/**
 * Port interface for template management operations
 * Defines the contract that infrastructure adapters must implement
 */
public interface TemplateManagementPort {
    /**
     * Create a new notification template
     *
     * @param request the template creation request
     * @return the created template response
     */
    TemplateResponse createTemplate(CreateTemplateRequest request);

    /**
     * Retrieve a template by slug, language, and channel
     *
     * @param slug the template slug
     * @param language the template language
     * @param channel the notification channel
     * @return the template response
     */
    TemplateResponse getTemplate(String slug, String language, Channel channel);

    /**
     * Update an existing template
     *
     * @param slug the template slug
     * @param language the template language
     * @param channel the notification channel
     * @param request the update request
     * @return the updated template response
     */
    TemplateResponse updateTemplate(String slug, String language, Channel channel, UpdateTemplateRequest request);

    /**
     * Delete a template
     *
     * @param slug the template slug
     * @param language the template language
     * @param channel the notification channel
     */
    void deleteTemplate(String slug, String language, Channel channel);
}
