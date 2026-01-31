package com.vibe.notification.infrastructure.adapter.persistence;

import com.vibe.notification.domain.dto.NotificationLogDTO;
import com.vibe.notification.domain.port.NotificationLogPort;
import com.vibe.notification.infrastructure.adapter.mapper.NotificationLogMapper;
import com.vibe.notification.infrastructure.persistence.entity.NotificationLogEntity;
import com.vibe.notification.infrastructure.persistence.repository.NotificationLogRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence adapter implementing NotificationLogPort
 * Maps between domain DTOs and infrastructure entities
 */
@Component
public class NotificationLogPersistenceAdapter implements NotificationLogPort {
    private final NotificationLogRepository repository;
    private final NotificationLogMapper mapper;

    public NotificationLogPersistenceAdapter(
            NotificationLogRepository repository,
            NotificationLogMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public NotificationLogDTO save(NotificationLogDTO dto) {
        NotificationLogEntity entity = mapper.dtoToEntity(dto);
        NotificationLogEntity saved = repository.save(entity);
        return mapper.entityToDto(saved);
    }

    @Override
    public Optional<NotificationLogDTO> findById(UUID id) {
        return repository.findById(id)
                .map(mapper::entityToDto);
    }
}
