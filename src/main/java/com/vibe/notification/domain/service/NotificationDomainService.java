package com.vibe.notification.domain.service;

import com.vibe.notification.domain.model.NotificationRequest;
import com.vibe.notification.domain.model.NotificationStatus;
import com.vibe.notification.infrastructure.persistence.entity.NotificationLogEntity;
import com.vibe.notification.infrastructure.persistence.repository.NotificationLogRepository;
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

    private final NotificationLogRepository notificationLogRepository;
    private final ObjectMapper objectMapper;

    public NotificationDomainService(NotificationLogRepository notificationLogRepository, ObjectMapper objectMapper) {
        this.notificationLogRepository = notificationLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Create pending notification log entry
     */
    public NotificationLogEntity createPendingLog(NotificationRequest request, UUID traceId) {
        logger.debug("Creating pending log for trace_id={}, recipient={}", traceId, request.recipient());

        var logId = UUID.randomUUID();
        var variablesJson = objectMapper.valueToTree(request.variables());

        var logEntity = new NotificationLogEntity(
            logId,
            traceId,
            request.recipient(),
            request.slug(),
            request.channel().name(),
            variablesJson,
            NotificationStatus.PENDING.name()
        );

        return notificationLogRepository.save(logEntity);
    }

    /**
     * Mark notification as successfully sent
     */
    public void markAsSent(UUID logId) {
        var log = notificationLogRepository.findById(logId)
            .orElseThrow(() -> new IllegalArgumentException("Log not found: " + logId));
        
        log.setStatus(NotificationStatus.SUCCESS.name());
        log.setSentAt(LocalDateTime.now());
        notificationLogRepository.save(log);
        
        logger.info("Notification marked as sent: logId={}", logId);
    }

    /**
     * Mark notification as failed with error message
     */
    public void markAsFailed(UUID logId, String errorMessage) {
        var log = notificationLogRepository.findById(logId)
            .orElseThrow(() -> new IllegalArgumentException("Log not found: " + logId));
        
        log.setStatus(NotificationStatus.FAILED.name());
        log.setErrorMessage(errorMessage);
        log.setSentAt(LocalDateTime.now());
        notificationLogRepository.save(log);
        
        logger.error("Notification marked as failed: logId={}, error={}", logId, errorMessage);
    }
}
