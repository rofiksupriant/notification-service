package com.vibe.notification.infrastructure.persistence.repository;

import com.vibe.notification.infrastructure.persistence.entity.NotificationTemplateEntity;
import com.vibe.notification.infrastructure.persistence.entity.NotificationTemplateId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplateEntity, NotificationTemplateId> {
    
    /**
     * Find template by slug and language
     */
    Optional<NotificationTemplateEntity> findById(NotificationTemplateId id);

    /**
     * Find all templates for a specific slug (different languages)
     */
    @Query("SELECT t FROM NotificationTemplateEntity t WHERE t.id.slug = :slug")
    List<NotificationTemplateEntity> findAllBySlug(String slug);

    /**
     * Find templates by channel and type
     */
    List<NotificationTemplateEntity> findByChannelAndTemplateType(String channel, String templateType);
}
