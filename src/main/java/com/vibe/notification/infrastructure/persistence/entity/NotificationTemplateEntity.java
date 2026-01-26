package com.vibe.notification.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_templates")
public class NotificationTemplateEntity {
    
    @EmbeddedId
    private NotificationTemplateId id;

    @Column(name = "channel", nullable = false)
    private String channel;

    @Column(name = "template_type", nullable = false)
    private String templateType;

    @Column(name = "subject")
    private String subject;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_url")
    private String imageUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public NotificationTemplateEntity() {}

    public NotificationTemplateEntity(NotificationTemplateId id, String channel, String templateType,
                                     String subject, String content, String imageUrl) {
        this.id = id;
        this.channel = channel;
        this.templateType = templateType;
        this.subject = subject;
        this.content = content;
        this.imageUrl = imageUrl;
    }

    // Getters & Setters
    public NotificationTemplateId getId() {
        return id;
    }

    public void setId(NotificationTemplateId id) {
        this.id = id;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
