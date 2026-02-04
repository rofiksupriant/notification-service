package com.vibe.notification.application;

import com.vibe.notification.application.dto.SendNotificationRequest;
import com.vibe.notification.application.dto.NotificationResponse;
import com.vibe.notification.application.port.EmailNotificationPort;
import com.vibe.notification.application.port.WhatsAppNotificationPort;
import com.vibe.notification.application.port.IdempotencyPort;
import com.vibe.notification.domain.model.Channel;
import com.vibe.notification.domain.model.NotificationRequest;
import com.vibe.notification.domain.model.NotificationResult;
import com.vibe.notification.domain.model.NotificationStatusEvent;
import com.vibe.notification.domain.port.NotificationStatusProducer;
import com.vibe.notification.domain.service.TraceService;
import com.vibe.notification.domain.service.NotificationDomainService;
import com.vibe.notification.domain.service.TemplateResolutionService;
import com.vibe.notification.domain.service.TemplateRenderingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Application service for notification processing orchestration
 * Handles the complete flow: request -> idempotency check -> trace -> render -> send -> audit
 */
@Service
public class NotificationApplicationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationApplicationService.class);

    private final TraceService traceService;
    private final NotificationDomainService notificationDomainService;
    private final TemplateResolutionService templateResolutionService;
    private final TemplateRenderingService templateRenderingService;
    private final EmailNotificationPort emailNotificationPort;
    private final WhatsAppNotificationPort whatsAppNotificationPort;
    private final IdempotencyPort idempotencyPort;
    private final NotificationStatusProducer notificationStatusProducer;
    private final NotificationApplicationService self;

    public NotificationApplicationService(
        TraceService traceService,
        NotificationDomainService notificationDomainService,
        TemplateResolutionService templateResolutionService,
        TemplateRenderingService templateRenderingService,
        EmailNotificationPort emailNotificationPort,
        WhatsAppNotificationPort whatsAppNotificationPort,
        IdempotencyPort idempotencyPort,
        NotificationStatusProducer notificationStatusProducer,
        @Lazy NotificationApplicationService self) {
        this.traceService = traceService;
        this.notificationDomainService = notificationDomainService;
        this.templateResolutionService = templateResolutionService;
        this.templateRenderingService = templateRenderingService;
        this.emailNotificationPort = emailNotificationPort;
        this.whatsAppNotificationPort = whatsAppNotificationPort;
        this.idempotencyPort = idempotencyPort;
        this.notificationStatusProducer = notificationStatusProducer;
        this.self = self;
    }

    /**
     * Send notification (synchronously returns response with trace_id)
     * Actual delivery happens asynchronously.
     * 
     * Supports idempotent processing: if request.traceId() is provided, checks if already processed.
     * If not provided, generates a new trace ID internally.
     */
    public NotificationResponse sendNotification(SendNotificationRequest request) {
        logger.info("Processing notification request: recipient={}, slug={}", request.recipient(), request.slug());

        // Check idempotency if client provided a trace ID
        String clientTraceIdStr = null;
        if (request.traceId().isPresent()) {
            clientTraceIdStr = request.traceId().get();
            
            // Check if this request was already processed
            if (isMessageAlreadyProcessed(clientTraceIdStr)) {
                logger.debug("Request with traceId {} already processed, skipping", clientTraceIdStr);
                // Return a response indicating it was already processed
                return new NotificationResponse(
                    null,
                    clientTraceIdStr,
                    "ALREADY_PROCESSED",
                    "This request was already processed"
                );
            }
            
            // Mark as processed before creating log (for idempotency)
            markMessageAsProcessed(clientTraceIdStr);
        }

        // Generate internal trace ID for logging
        var internalTraceId = traceService.generateTraceId();
        
        var notificationRequest = new NotificationRequest(
            request.recipient(),
            request.slug(),
            request.language(),
            request.channel(),
            request.variables()
        );

        // Create pending log entry with internal trace ID
        var logEntity = notificationDomainService.createPendingLog(notificationRequest, internalTraceId);

        // Execute async processing
        self.processNotificationAsync(logEntity.getId(), notificationRequest, internalTraceId);

        return new NotificationResponse(
            logEntity.getId(),
            internalTraceId.toString(),
            "PENDING",
            "Notification queued for processing"
        );
    }

    /**
     * Send notification with synchronous mode support
     * 
     * @param request the notification request
     * @param sync if true, waits for processing to complete (max 15s); if false, returns immediately
     * @return response with final status if sync=true, or pending status if sync=false
     */
    public NotificationResponse sendNotificationWithSync(SendNotificationRequest request, boolean sync) {
        logger.info("Processing notification request (sync={}): recipient={}, slug={}", sync, request.recipient(), request.slug());

        // Check idempotency if client provided a trace ID
        String clientTraceIdStr = null;
        if (request.traceId().isPresent()) {
            clientTraceIdStr = request.traceId().get();
            
            // Check if this request was already processed
            if (isMessageAlreadyProcessed(clientTraceIdStr)) {
                logger.debug("Request with traceId {} already processed, skipping", clientTraceIdStr);
                // Return a response indicating it was already processed
                return new NotificationResponse(
                    null,
                    clientTraceIdStr,
                    "ALREADY_PROCESSED",
                    "This request was already processed"
                );
            }
            
            // Mark as processed before creating log (for idempotency)
            markMessageAsProcessed(clientTraceIdStr);
        }

        // Generate internal trace ID for logging
        var internalTraceId = traceService.generateTraceId();
        
        var notificationRequest = new NotificationRequest(
            request.recipient(),
            request.slug(),
            request.language(),
            request.channel(),
            request.variables()
        );

        // Create pending log entry with internal trace ID
        var logEntity = notificationDomainService.createPendingLog(notificationRequest, internalTraceId);

        if (sync) {
            // Synchronous mode: wait for completion
            try {
                CompletableFuture<NotificationResult> future = self.processNotificationWithResult(logEntity.getId(), notificationRequest, internalTraceId);
                
                // Wait for completion with 15-second timeout
                NotificationResult result = future.get(15, java.util.concurrent.TimeUnit.SECONDS);
                
                return new NotificationResponse(
                    logEntity.getId(),
                    internalTraceId.toString(),
                    result.status().name(),
                    result.isSuccess() ? "Notification sent successfully" : "Notification failed: " + result.errorMessage(),
                    result.status()
                );
            } catch (java.util.concurrent.TimeoutException e) {
                logger.warn("Notification processing timed out after 15 seconds: logId={}", logEntity.getId());
                return new NotificationResponse(
                    logEntity.getId(),
                    internalTraceId.toString(),
                    "TIMEOUT",
                    "Notification processing timed out after 15 seconds"
                );
            } catch (Exception e) {
                logger.error("Error waiting for notification completion: logId={}, error={}", logEntity.getId(), e.getMessage(), e);
                return new NotificationResponse(
                    logEntity.getId(),
                    internalTraceId.toString(),
                    "ERROR",
                    "Error processing notification: " + e.getMessage()
                );
            }
        } else {
            // Asynchronous mode: return immediately
            self.processNotificationAsync(logEntity.getId(), notificationRequest, internalTraceId);
            
            return new NotificationResponse(
                logEntity.getId(),
                internalTraceId.toString(),
                "ACCEPTED",
                "Notification accepted for processing"
            );
        }
    }

    /**
     * Checks if a message with the given trace_id has already been processed.
     *
     * @param traceId the unique message identifier
     * @return true if the message was previously processed, false otherwise
     */
    private boolean isMessageAlreadyProcessed(String traceId) {
        return idempotencyPort.isMessageAlreadyProcessed(traceId);
    }

    /**
     * Records that a message has been successfully processed.
     *
     * @param traceId the unique message identifier
     */
    private void markMessageAsProcessed(String traceId) {
        idempotencyPort.markMessageAsProcessed(traceId);
    }

    /**
     * Async notification processing with trace_id in MDC
     */
    @Async
    public void processNotificationAsync(java.util.UUID logId, NotificationRequest request, UUID traceId) {
        String traceIdStr = traceId.toString();
        try {
            logger.info("Starting async notification processing: logId={}", logId);

            // Resolve template with language fallback
            var template = templateResolutionService.resolveTemplate(request.slug(), request.language(), request.channel());

            // Render template content
            var renderedContent = templateRenderingService.renderContent(template.getContent(), request.variables());
            var renderedSubject = templateRenderingService.renderSubject(template.getSubject(), request.variables());

            // Send via appropriate port
            switch (request.channel()) {
                case EMAIL -> emailNotificationPort.sendEmail(request.recipient(), template, renderedSubject, renderedContent);
                case WHATSAPP -> whatsAppNotificationPort.sendWhatsAppMessage(request.recipient(), template, renderedContent);
                default -> {
                    logger.error("Notification processing failed: logId={}, error={}", logId, "Unsupported channel: " + request.channel());
                    notificationDomainService.markAsFailed(logId, "Unsupported channel: " + request.channel());
                    // Publish FAILED status
                    publishStatusSafely(logId, request.channel(), NotificationStatusEvent.failure(
                        traceIdStr, request.channel(), "Unsupported channel: " + request.channel()));
                    return;
                }
            }

            // Mark as successfully sent
            notificationDomainService.markAsSent(logId);
            logger.info("Notification processed successfully: logId={}", logId);
            
            // Publish SUCCESS status
            publishStatusSafely(logId, request.channel(), NotificationStatusEvent.success(
                traceIdStr, request.channel()));

        } catch (Exception e) {
            logger.error("Notification processing failed: logId={}, error={}", logId, e.getMessage(), e);
            notificationDomainService.markAsFailed(logId, e.getMessage());
            
            // Publish FAILED status
            publishStatusSafely(logId, request.channel(), NotificationStatusEvent.failure(
                traceIdStr, request.channel(), e.getMessage()));
        } finally {
            traceService.clearTraceId();
        }
    }

    /**
     * Async notification processing that returns a CompletableFuture with the result
     */
    @Async
    public CompletableFuture<NotificationResult> processNotificationWithResult(UUID logId, NotificationRequest request, UUID traceId) {
        String traceIdStr = traceId.toString();
        try {
            logger.info("Starting async notification processing with result: logId={}", logId);

            // Resolve template with language fallback
            var template = templateResolutionService.resolveTemplate(request.slug(), request.language(), request.channel());

            // Render template content
            var renderedContent = templateRenderingService.renderContent(template.getContent(), request.variables());
            var renderedSubject = templateRenderingService.renderSubject(template.getSubject(), request.variables());

            // Send via appropriate port
            switch (request.channel()) {
                case EMAIL -> emailNotificationPort.sendEmail(request.recipient(), template, renderedSubject, renderedContent);
                case WHATSAPP -> whatsAppNotificationPort.sendWhatsAppMessage(request.recipient(), template, renderedContent);
                default -> {
                    String error = "Unsupported channel: " + request.channel();
                    logger.error("Notification processing failed: logId={}, error={}", logId, error);
                    notificationDomainService.markAsFailed(logId, error);
                    // Publish FAILED status
                    publishStatusSafely(logId, request.channel(), NotificationStatusEvent.failure(
                        traceIdStr, request.channel(), error));
                    return CompletableFuture.completedFuture(NotificationResult.failure(error));
                }
            }

            // Mark as successfully sent
            notificationDomainService.markAsSent(logId);
            logger.info("Notification processed successfully: logId={}", logId);
            
            // Publish SUCCESS status
            publishStatusSafely(logId, request.channel(), NotificationStatusEvent.success(
                traceIdStr, request.channel()));
            
            return CompletableFuture.completedFuture(NotificationResult.success());

        } catch (Exception e) {
            logger.error("Notification processing failed: logId={}, error={}", logId, e.getMessage(), e);
            notificationDomainService.markAsFailed(logId, e.getMessage());
            
            // Publish FAILED status
            publishStatusSafely(logId, request.channel(), NotificationStatusEvent.failure(
                traceIdStr, request.channel(), e.getMessage()));
            
            return CompletableFuture.completedFuture(NotificationResult.failure(e.getMessage()));
        } finally {
            traceService.clearTraceId();
        }
    }
    
    /**
     * Safely publishes status event, catching any exceptions to prevent failures
     */
    private void publishStatusSafely(UUID logId, Channel channel, NotificationStatusEvent event) {
        try {
            notificationStatusProducer.publishStatus(event);
        } catch (Exception e) {
            // Log but don't fail the notification - status publishing is auxiliary
            logger.warn("Failed to publish status event for logId={}: {}", logId, e.getMessage());
        }
    }
}
