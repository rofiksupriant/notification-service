package com.vibe.notification.infrastructure.adapter.persistence;

import com.vibe.notification.domain.dto.TemplateDTO;
import com.vibe.notification.domain.dto.TemplateIdDTO;
import com.vibe.notification.domain.port.NotificationTemplatePort;
import com.vibe.notification.infrastructure.adapter.mapper.TemplateMapper;
import com.vibe.notification.infrastructure.persistence.entity.NotificationTemplateId;
import com.vibe.notification.infrastructure.persistence.repository.NotificationTemplateRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Persistence adapter implementing NotificationTemplatePort
 * Maps between domain DTOs and infrastructure entities
 */
@Component
public class TemplatePersistenceAdapter implements NotificationTemplatePort {
    private final NotificationTemplateRepository repository;
    private final TemplateMapper mapper;

    public TemplatePersistenceAdapter(
            NotificationTemplateRepository repository,
            TemplateMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<TemplateDTO> findById(TemplateIdDTO id) {
        NotificationTemplateId entityId = mapper.toIdEntity(id);
        return repository.findById(entityId)
                .map(mapper::entityToDto);
    }

    @Override
    public TemplateDTO save(TemplateDTO dto) {
        var entity = mapper.dtoToEntity(dto);
        var saved = repository.save(entity);
        return mapper.entityToDto(saved);
    }
}
