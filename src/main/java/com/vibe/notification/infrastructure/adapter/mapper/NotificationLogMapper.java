package com.vibe.notification.infrastructure.adapter.mapper;

import com.vibe.notification.domain.dto.NotificationLogDTO;
import com.vibe.notification.infrastructure.persistence.entity.NotificationLogEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper for NotificationLog between entity and DTO
 */
@Component
public class NotificationLogMapper {

    public NotificationLogDTO entityToDto(NotificationLogEntity entity) {
        if (entity == null) {
            return null;
        }
        return new NotificationLogDTO(
                entity.getId(),
                entity.getTraceId(),
                entity.getSlug(),
                "",  // language not in entity, use empty string
                entity.getChannel(),
                entity.getRecipient(),
                entity.getVariables(),
                entity.getStatus(),
                entity.getErrorMessage(),
                entity.getSentAt(),
                entity.getCreatedAt(),
                entity.getCreatedAt()  // updatedAt not in entity, use createdAt
        );
    }

    public NotificationLogEntity dtoToEntity(NotificationLogDTO dto) {
        if (dto == null) {
            return null;
        }
        NotificationLogEntity entity = new NotificationLogEntity(
                dto.getId(),
                dto.getTraceId(),
                dto.getRecipient(),
                dto.getSlug(),
                dto.getChannel(),
                dto.getVariables(),
                dto.getStatus()
        );
        entity.setErrorMessage(dto.getErrorMessage());
        entity.setSentAt(dto.getSentAt());
        return entity;
    }
}
