package com.vibe.notification.presentation.controller;

import com.vibe.notification.application.dto.CreateTemplateRequest;
import com.vibe.notification.application.dto.TemplateResponse;
import com.vibe.notification.application.dto.UpdateTemplateRequest;
import com.vibe.notification.infrastructure.adapter.TemplateManagementAdapter;
import com.vibe.notification.infrastructure.adapter.security.ApiKeyValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Template Management CRUD API
 * All endpoints require X-API-Key header for authorization
 */
@RestController
@RequestMapping("/api/v1/templates")
public class TemplateController {
    private static final Logger logger = LoggerFactory.getLogger(TemplateController.class);
    private static final String API_KEY_HEADER = "X-API-Key";

    private final TemplateManagementAdapter templateManagementAdapter;
    private final ApiKeyValidator apiKeyValidator;

    public TemplateController(
        TemplateManagementAdapter templateManagementAdapter,
        ApiKeyValidator apiKeyValidator) {
        this.templateManagementAdapter = templateManagementAdapter;
        this.apiKeyValidator = apiKeyValidator;
    }

    /**
     * Create a new notification template
     * POST /api/v1/templates
     *
     * @param request the template creation request
     * @param apiKey the API key for authorization
     * @return the created template with 201 Created status
     */
    @PostMapping
    public ResponseEntity<TemplateResponse> createTemplate(
        @RequestBody CreateTemplateRequest request,
        @RequestHeader(API_KEY_HEADER) String apiKey) {
        
        logger.info("Received template creation request: slug={}, language={}", 
            request.slug(), request.language());

        if (!apiKeyValidator.validateApiKey(apiKey)) {
            logger.warn("Template creation rejected: unauthorized API key");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var response = templateManagementAdapter.createTemplate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Fetch a specific template by slug and language
     * GET /api/v1/templates/{slug}/{lang}
     *
     * @param slug the template slug
     * @param lang the template language
     * @return the template response
     */
    @GetMapping("/{slug}/{lang}")
    public ResponseEntity<TemplateResponse> getTemplate(
        @PathVariable String slug,
        @PathVariable String lang) {
        
        logger.debug("Received template fetch request: slug={}, language={}", slug, lang);

        var response = templateManagementAdapter.getTemplate(slug, lang);
        return ResponseEntity.ok(response);
    }

    /**
     * Update an existing template
     * PUT /api/v1/templates/{slug}/{lang}
     *
     * @param slug the template slug
     * @param lang the template language
     * @param request the template update request
     * @param apiKey the API key for authorization
     * @return the updated template response
     */
    @PutMapping("/{slug}/{lang}")
    public ResponseEntity<TemplateResponse> updateTemplate(
        @PathVariable String slug,
        @PathVariable String lang,
        @RequestBody UpdateTemplateRequest request,
        @RequestHeader(API_KEY_HEADER) String apiKey) {
        
        logger.info("Received template update request: slug={}, language={}", slug, lang);

        if (!apiKeyValidator.validateApiKey(apiKey)) {
            logger.warn("Template update rejected: unauthorized API key");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var response = templateManagementAdapter.updateTemplate(slug, lang, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a template
     * DELETE /api/v1/templates/{slug}/{lang}
     *
     * @param slug the template slug
     * @param lang the template language
     * @param apiKey the API key for authorization
     * @return 204 No Content response
     */
    @DeleteMapping("/{slug}/{lang}")
    public ResponseEntity<Void> deleteTemplate(
        @PathVariable String slug,
        @PathVariable String lang,
        @RequestHeader(API_KEY_HEADER) String apiKey) {
        
        logger.info("Received template deletion request: slug={}, language={}", slug, lang);

        if (!apiKeyValidator.validateApiKey(apiKey)) {
            logger.warn("Template deletion rejected: unauthorized API key");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        templateManagementAdapter.deleteTemplate(slug, lang);
        return ResponseEntity.noContent().build();
    }
}
