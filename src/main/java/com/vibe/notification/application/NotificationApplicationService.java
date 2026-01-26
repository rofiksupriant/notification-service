package com.vibe.notification.application;

import com.vibe.notification.application.dto.SendNotificationRequest;
import com.vibe.notification.application.dto.NotificationResponse;
import com.vibe.notification.domain.model.Channel;
import com.vibe.notification.domain.model.NotificationRequest;
import com.vibe.notification.domain.service.TraceService;
import com.vibe.notification.infrastructure.adapter.email.EmailNotificationAdapter;
import com.vibe.notification.infrastructure.adapter.whatsapp.WhatsAppNotificationAdapter;
import com.vibe.notification.domain.service.NotificationDomainService;
import com.vibe.notification.domain.service.TemplateResolutionService;
import com.vibe.notification.domain.service.TemplateRenderingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Application service for notification processing orchestration
 * Handles the complete flow: request -> trace -> render -> send -> audit
 */
@Service
public class NotificationApplicationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationApplicationService.class);

    private final TraceService traceService;
    private final NotificationDomainService notificationDomainService;
    private final TemplateResolutionService templateResolutionService;
    private final TemplateRenderingService templateRenderingService;
    private final EmailNotificationAdapter emailNotificationAdapter;
    private final WhatsAppNotificationAdapter whatsAppNotificationAdapter;

    public NotificationApplicationService(
        TraceService traceService,
        NotificationDomainService notificationDomainService,
        TemplateResolutionService templateResolutionService,
        TemplateRenderingService templateRenderingService,
        EmailNotificationAdapter emailNotificationAdapter,
        WhatsAppNotificationAdapter whatsAppNotificationAdapter) {
        this.traceService = traceService;
        this.notificationDomainService = notificationDomainService;
        this.templateResolutionService = templateResolutionService;
        this.templateRenderingService = templateRenderingService;
        this.emailNotificationAdapter = emailNotificationAdapter;
        this.whatsAppNotificationAdapter = whatsAppNotificationAdapter;
    }

    /**
     * Send notification (synchronously returns response with trace_id)
     * Actual delivery happens asynchronously
     */
    public NotificationResponse sendNotification(SendNotificationRequest request) {
        logger.info("Processing notification request: recipient={}, slug={}", request.recipient(), request.slug());

        var traceId = traceService.generateTraceId();
        var channel = Channel.from(request.channel());
        
        var notificationRequest = new NotificationRequest(
            request.recipient(),
            request.slug(),
            request.language(),
            channel,
            request.variables()
        );

        // Create pending log entry
        var logEntity = notificationDomainService.createPendingLog(notificationRequest, traceId);

        // Execute async processing
        processNotificationAsync(logEntity.getId(), notificationRequest);

        return new NotificationResponse(
            logEntity.getId(),
            traceId,
            "PENDING",
            "Notification queued for processing"
        );
    }

    /**
     * Async notification processing with trace_id in MDC
     */
    @Async
    public void processNotificationAsync(java.util.UUID logId, NotificationRequest request) {
        var traceId = traceService.generateTraceId();
        try {
            logger.info("Starting async notification processing: logId={}", logId);

            // Resolve template with language fallback
            var template = templateResolutionService.resolveTemplate(request.slug(), request.language());

            // Render template content
            var renderedContent = templateRenderingService.renderContent(template.getContent(), request.variables());
            var renderedSubject = templateRenderingService.renderSubject(template.getSubject(), request.variables());

            // Send via appropriate adapter
            switch (request.channel()) {
                case EMAIL -> emailNotificationAdapter.sendEmail(request.recipient(), template, renderedContent, renderedSubject);
                case WHATSAPP -> whatsAppNotificationAdapter.sendWhatsAppMessage(request.recipient(), template, renderedContent);
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
