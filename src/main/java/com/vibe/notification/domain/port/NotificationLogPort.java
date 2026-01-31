package com.vibe.notification.domain.port;

import com.vibe.notification.domain.dto.NotificationLogDTO;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for NotificationLog persistence operations.
 * Domain layer defines the contract; infrastructure implements it.
 */
public interface NotificationLogPort {
    NotificationLogDTO save(NotificationLogDTO logEntity);
    Optional<NotificationLogDTO> findById(UUID id);
}
