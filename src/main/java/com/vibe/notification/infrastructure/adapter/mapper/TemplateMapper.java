package com.vibe.notification.infrastructure.adapter.mapper;

import com.vibe.notification.domain.dto.TemplateDTO;
import com.vibe.notification.domain.dto.TemplateIdDTO;
import com.vibe.notification.infrastructure.persistence.entity.NotificationTemplateEntity;
import com.vibe.notification.infrastructure.persistence.entity.NotificationTemplateId;
import org.springframework.stereotype.Component;

/**
 * Mapper for NotificationTemplate between entity and DTO
 */
@Component
public class TemplateMapper {

    public TemplateDTO entityToDto(NotificationTemplateEntity entity) {
        if (entity == null) {
            return null;
        }
        TemplateIdDTO idDto = new TemplateIdDTO(
                entity.getId().getSlug(),
                entity.getId().getLanguage(),
                entity.getId().getChannel()
        );
        return new TemplateDTO(
                idDto,
                entity.getId().getSlug(),
                entity.getId().getLanguage(),
                entity.getChannel(),
                entity.getTemplateType(),
                entity.getSubject(),
                entity.getContent(),
                entity.getImageUrl(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public NotificationTemplateEntity dtoToEntity(TemplateDTO dto) {
        if (dto == null) {
            return null;
        }
        NotificationTemplateId id = new NotificationTemplateId(
                dto.getSlug(),
                dto.getLanguage(),
                dto.getChannel()
        );
        return new NotificationTemplateEntity(
                id,
                dto.getType(),
                dto.getSubject(),
                dto.getContent(),
                dto.getImageUrl()
        );
    }

    public TemplateIdDTO toIdDto(NotificationTemplateId id) {
        if (id == null) {
            return null;
        }
        return new TemplateIdDTO(id.getSlug(), id.getLanguage(), id.getChannel());
    }

    public NotificationTemplateId toIdEntity(TemplateIdDTO idDto) {
        if (idDto == null) {
            return null;
        }
        return new NotificationTemplateId(idDto.getSlug(), idDto.getLanguage(), idDto.getChannel());
    }
}
