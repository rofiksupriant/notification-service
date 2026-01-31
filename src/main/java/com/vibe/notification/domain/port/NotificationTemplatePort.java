package com.vibe.notification.domain.port;

import com.vibe.notification.domain.dto.TemplateDTO;
import com.vibe.notification.domain.dto.TemplateIdDTO;

import java.util.Optional;

/**
 * Port for NotificationTemplate persistence operations.
 * Domain layer defines the contract; infrastructure implements it.
 */
public interface NotificationTemplatePort {
    Optional<TemplateDTO> findById(TemplateIdDTO id);
    TemplateDTO save(TemplateDTO templateEntity);
}
