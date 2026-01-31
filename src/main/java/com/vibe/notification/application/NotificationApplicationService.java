package com.vibe.notification.application;

import com.vibe.notification.application.dto.SendNotificationRequest;
import com.vibe.notification.application.dto.NotificationResponse;
import com.vibe.notification.application.port.EmailNotificationPort;
import com.vibe.notification.application.port.WhatsAppNotificationPort;
import com.vibe.notification.application.port.IdempotencyPort;
import com.vibe.notification.domain.model.NotificationRequest;
import com.vibe.notification.domain.service.TraceService;
import com.vibe.notification.domain.service.NotificationDomainService;
import com.vibe.notification.domain.service.TemplateResolutionService;
import com.vibe.notification.domain.service.TemplateRenderingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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
    private final NotificationApplicationService self;

    public NotificationApplicationService(
        TraceService traceService,
        NotificationDomainService notificationDomainService,
        TemplateResolutionService templateResolutionService,
        TemplateRenderingService templateRenderingService,
        EmailNotificationPort emailNotificationPort,
        WhatsAppNotificationPort whatsAppNotificationPort,
        IdempotencyPort idempotencyPort,
        @Lazy NotificationApplicationService self) {
        this.traceService = traceService;
        this.notificationDomainService = notificationDomainService;
        this.templateResolutionService = templateResolutionService;
        this.templateRenderingService = templateRenderingService;
        this.emailNotificationPort = emailNotificationPort;
        this.whatsAppNotificationPort = whatsAppNotificationPort;
        this.idempotencyPort = idempotencyPort;
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
        self.processNotificationAsync(logEntity.getId(), notificationRequest);

        return new NotificationResponse(
            logEntity.getId(),
            internalTraceId.toString(),
            "PENDING",
            "Notification queued for processing"
        );
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
    public void processNotificationAsync(java.util.UUID logId, NotificationRequest request) {
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
                default -> logger.error("Notification processing failed: logId={}, error={}", logId, "Unsupported channel: " + request.channel());
            }

            // Mark as successfully sent
            notificationDomainService.markAsSent(logId);
            logger.info("Notification processed successfully: logId={}", logId);

        } catch (Exception e) {
            logger.error("Notification processing failed: logId={}, error={}", logId, e.getMessage(), e);
            notificationDomainService.markAsFailed(logId, e.getMessage());
        } finally {
            traceService.clearTraceId();
        }
    }
}
