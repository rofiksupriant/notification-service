package com.vibe.notification.domain.service;

import com.vibe.notification.domain.dto.NotificationLogDTO;
import com.vibe.notification.domain.model.NotificationRequest;
import com.vibe.notification.domain.model.NotificationResult;
import com.vibe.notification.domain.model.NotificationStatus;
import com.vibe.notification.domain.port.NotificationLogPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain service for notification processing orchestration
 */
@Service
public class NotificationDomainService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationDomainService.class);

    private final NotificationLogPort notificationLogPort;
    private final ObjectMapper objectMapper;

    public NotificationDomainService(NotificationLogPort notificationLogPort, ObjectMapper objectMapper) {
        this.notificationLogPort = notificationLogPort;
        this.objectMapper = objectMapper;
    }

    /**
     * Create pending notification log entry
     */
    public NotificationLogDTO createPendingLog(NotificationRequest request, UUID traceId) {
        logger.debug("Creating pending log for trace_id={}, recipient={}", traceId, request.recipient());

        var logId = UUID.randomUUID();
        var variablesJson = objectMapper.valueToTree(request.variables());

        var logDto = new NotificationLogDTO(
            logId,
            traceId,
            request.slug(),
            request.language(),
            request.channel().name(),
            request.recipient(),
            variablesJson,
            NotificationStatus.PENDING.name(),
            null,
            null,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        return notificationLogPort.save(logDto);
    }

    /**
     * Mark notification as successfully sent
     */
    public void markAsSent(UUID logId) {
        var log = notificationLogPort.findById(logId)
            .orElseThrow(() -> new IllegalArgumentException("Log not found: " + logId));
        
        var updatedLog = new NotificationLogDTO(
            log.getId(),
            log.getTraceId(),
            log.getSlug(),
            log.getLanguage(),
            log.getChannel(),
            log.getRecipient(),
            log.getVariables(),
            NotificationStatus.SUCCESS.name(),
            null,
            LocalDateTime.now(),
            log.getCreatedAt(),
            LocalDateTime.now()
        );
        
        notificationLogPort.save(updatedLog);
        logger.info("Notification marked as sent: logId={}", logId);
    }

    /**
     * Mark notification as failed with error message
     */
    public void markAsFailed(UUID logId, String errorMessage) {
        var log = notificationLogPort.findById(logId)
            .orElseThrow(() -> new IllegalArgumentException("Log not found: " + logId));
        
        var updatedLog = new NotificationLogDTO(
            log.getId(),
            log.getTraceId(),
            log.getSlug(),
            log.getLanguage(),
            log.getChannel(),
            log.getRecipient(),
            log.getVariables(),
            NotificationStatus.FAILED.name(),
            errorMessage,
            LocalDateTime.now(),
            log.getCreatedAt(),
            LocalDateTime.now()
        );
        
        notificationLogPort.save(updatedLog);
        logger.error("Notification marked as failed: logId={}, error={}", logId, errorMessage);
    }

    /**
     * Get the final result for a notification by log ID
     */
    public NotificationResult getNotificationResult(UUID logId) {
        var log = notificationLogPort.findById(logId)
            .orElseThrow(() -> new IllegalArgumentException("Log not found: " + logId));
        
        NotificationStatus status = NotificationStatus.fromString(log.getStatus());
        return new NotificationResult(status, log.getErrorMessage());
    }
    
    /**
     * Get notification log by ID
     */
    public NotificationLogDTO getNotificationLog(UUID logId) {
        return notificationLogPort.findById(logId)
            .orElseThrow(() -> new IllegalArgumentException("Log not found: " + logId));
    }
}
