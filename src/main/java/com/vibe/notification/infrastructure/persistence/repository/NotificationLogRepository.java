package com.vibe.notification.infrastructure.persistence.repository;

import com.vibe.notification.infrastructure.persistence.entity.NotificationLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLogEntity, UUID> {
    
    /**
     * Find all logs by trace_id
     */
    List<NotificationLogEntity> findByTraceId(UUID traceId);

    /**
     * Find logs by recipient
     */
    List<NotificationLogEntity> findByRecipient(String recipient);

    /**
     * Find logs by status
     */
    List<NotificationLogEntity> findByStatus(String status);
}
