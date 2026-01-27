package com.vibe.notification.presentation.controller;

import com.vibe.notification.application.dto.CreateTemplateRequest;
import com.vibe.notification.application.dto.TemplateResponse;
import com.vibe.notification.application.dto.UpdateTemplateRequest;
import com.vibe.notification.infrastructure.adapter.TemplateManagementAdapter;
import com.vibe.notification.infrastructure.adapter.security.ApiKeyValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Template Management", description = "APIs for managing notification templates")
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
    @Operation(summary = "Create a new notification template", 
               description = "Creates a new notification template with comprehensive support for multiple channels and languages. " +
                       "Templates support variable interpolation using {variable_name} syntax. " +
                       "Requires API Key authentication. Each slug-language combination must be unique.")
    @SecurityRequirement(name = "X-API-Key")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Template created successfully",
                     content = @Content(schema = @Schema(implementation = TemplateResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request body - Missing required fields (slug, language, channel, content) or invalid format"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid, missing, or expired API Key"),
        @ApiResponse(responseCode = "409", description = "Template already exists - A template with this slug and language combination already exists"),
        @ApiResponse(responseCode = "422", description = "Unprocessable Entity - Validation failed (e.g., unsupported channel, invalid language code)")
    })
    public ResponseEntity<TemplateResponse> createTemplate(
        @org.springframework.web.bind.annotation.RequestBody CreateTemplateRequest request,
        @RequestHeader(API_KEY_HEADER) @Parameter(description = "Valid API Key for authentication. Obtain from Vibe Dashboard.", required = true) String apiKey) {
        
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
    @Operation(summary = "Retrieve a template", 
               description = "Fetches a specific notification template by slug and language code. " +
                       "This endpoint does not require authentication and is suitable for read-only access. " +
                       "Returns complete template metadata including creation and modification timestamps.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Template retrieved successfully with all details",
                     content = @Content(schema = @Schema(implementation = TemplateResponse.class))),
        @ApiResponse(responseCode = "404", description = "Template not found - No template exists with the specified slug and language combination"),
        @ApiResponse(responseCode = "400", description = "Invalid parameters - Slug or language format is invalid")
    })
    public ResponseEntity<TemplateResponse> getTemplate(
        @PathVariable @Parameter(description = "Template slug identifier (alphanumeric with underscores)", example = "welcome_email") String slug,
        @PathVariable @Parameter(description = "ISO 639-1 language code", example = "en") String lang) {
        
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
    @Operation(summary = "Update an existing template", 
               description = "Updates an existing notification template with new subject and/or body content. " +
                       "Partial updates are supported - only provided fields will be updated. " +
                       "Requires API Key authentication. The updatedAt timestamp will be automatically refreshed.")
    @SecurityRequirement(name = "X-API-Key")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Template updated successfully with new metadata",
                     content = @Content(schema = @Schema(implementation = TemplateResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request body - Empty update payload or invalid field values"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid, missing, or expired API Key"),
        @ApiResponse(responseCode = "404", description = "Template not found - No template exists with the specified slug and language combination"),
        @ApiResponse(responseCode = "422", description = "Unprocessable Entity - Validation failed for updated fields")
    })
    public ResponseEntity<TemplateResponse> updateTemplate(
        @PathVariable @Parameter(description = "Template slug identifier (alphanumeric with underscores)", example = "welcome_email") String slug,
        @PathVariable @Parameter(description = "ISO 639-1 language code", example = "en") String lang,
        @org.springframework.web.bind.annotation.RequestBody UpdateTemplateRequest request,
        @RequestHeader(API_KEY_HEADER) @Parameter(description = "Valid API Key for authentication", required = true) String apiKey) {
        
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
    @Operation(summary = "Delete a template", 
               description = "Permanently deletes a notification template. This operation is irreversible. " +
                       "Once deleted, the template cannot be recovered unless restored from backups. " +
                       "Requires API Key authentication. Active notifications using this template will fail to send.")
    @SecurityRequirement(name = "X-API-Key")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Template deleted successfully. Returns empty body."),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid, missing, or expired API Key"),
        @ApiResponse(responseCode = "404", description = "Template not found - No template exists with the specified slug and language combination"),
        @ApiResponse(responseCode = "409", description = "Conflict - Template is currently in use by active notifications")
    })
    public ResponseEntity<Void> deleteTemplate(
        @PathVariable @Parameter(description = "Template slug identifier (alphanumeric with underscores)", example = "welcome_email") String slug,
        @PathVariable @Parameter(description = "ISO 639-1 language code", example = "en") String lang,
        @RequestHeader(API_KEY_HEADER) @Parameter(description = "Valid API Key for authentication", required = true) String apiKey) {
        
        logger.info("Received template deletion request: slug={}, language={}", slug, lang);

        if (!apiKeyValidator.validateApiKey(apiKey)) {
            logger.warn("Template deletion rejected: unauthorized API key");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        templateManagementAdapter.deleteTemplate(slug, lang);
        return ResponseEntity.noContent().build();
    }
}
