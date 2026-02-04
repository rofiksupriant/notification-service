package com.vibe.notification.presentation.controller;

import com.vibe.notification.application.NotificationApplicationService;
import com.vibe.notification.application.dto.SendNotificationRequest;
import com.vibe.notification.application.dto.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * REST Controller for Notification API
 */
@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notification Engine", description = "APIs for sending notifications")
public class NotificationController {
    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationApplicationService notificationApplicationService;

    public NotificationController(NotificationApplicationService notificationApplicationService) {
        this.notificationApplicationService = notificationApplicationService;
    }

    /**
     * Send notification endpoint
     * POST /api/v1/notifications/send
     */
    @PostMapping("/send")
    @Operation(summary = "Send a notification", 
               description = "Sends a notification (WhatsApp or Email) to the specified recipient with the given template and parameters. " +
                       "By default, processes asynchronously and returns 202 Accepted immediately. " +
                       "Use ?sync=true for synchronous processing (waits max 15s) and returns 200 OK with final provider status. " +
                       "Variable substitution is performed using the provided variables map. " +
                       "Supports optional Idempotency-Key header for idempotent request processing - if provided, ensures the request is processed only once.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Notification processed successfully (sync mode only) - Final provider status (SENT/FAILED) included in response",
                     content = @Content(schema = @Schema(implementation = NotificationResponse.class))),
        @ApiResponse(responseCode = "202", description = "Notification accepted for processing (async mode) - Use the returned notification ID to track status",
                     content = @Content(schema = @Schema(implementation = NotificationResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request body - Missing required fields (recipient, slug, language, channel) or invalid format"),
        @ApiResponse(responseCode = "422", description = "Unprocessable Entity - Template not found, validation failed, or recipient format is invalid")
    })
    public ResponseEntity<NotificationResponse> sendNotification(
        @RequestBody SendNotificationRequest request,
        @Parameter(name = "sync", description = "If true, waits for notification processing to complete (max 15s) and returns final status. If false (default), returns immediately with ACCEPTED status.")
        @RequestParam(value = "sync", defaultValue = "false") boolean sync,
        @Parameter(name = "Idempotency-Key", description = "Optional unique identifier for idempotent request processing. If provided, ensures the request is processed only once.")
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        logger.info("Received notification request (sync={}): {}", sync, request.recipient());
        
        // Create a new request with idempotency key if provided
        SendNotificationRequest enrichedRequest = new SendNotificationRequest(
            request.recipient(),
            request.slug(),
            request.language(),
            request.channel(),
            request.variables(),
            Optional.ofNullable(idempotencyKey)
        );
        
        var response = notificationApplicationService.sendNotificationWithSync(enrichedRequest, sync);
        
        // Return 200 OK for sync mode, 202 Accepted for async mode
        if (sync) {
            return ResponseEntity.ok().body(response);
        } else {
            return ResponseEntity.accepted().body(response);
        }
    }

    /**
     * Health check endpoint
     * GET /api/v1/notifications/health
     */
    @GetMapping("/health")
    @Operation(summary = "Health check endpoint", 
               description = "Checks if the Notification Service is running, healthy, and ready to accept requests. " +
                       "This endpoint can be used for liveness and readiness probes in Kubernetes deployments. " +
                       "Returns immediately without performing expensive checks.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Service is healthy and operational"),
        @ApiResponse(responseCode = "503", description = "Service unavailable or degraded - Database or critical dependencies are down")
    })
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Notification Service is healthy");
    }
}
