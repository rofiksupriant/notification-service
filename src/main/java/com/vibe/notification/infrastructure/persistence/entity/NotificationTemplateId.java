package com.vibe.notification.infrastructure.persistence.entity;

import com.vibe.notification.domain.model.Channel;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class NotificationTemplateId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "slug", nullable = false)
    private String slug;

    @Column(name = "language", nullable = false)
    private String language;

    @Column(name = "channel", nullable = false)
    @Enumerated(EnumType.STRING)
    private Channel channel;

    // Constructors
    public NotificationTemplateId() {
    }

    public NotificationTemplateId(String slug, String language, Channel channel) {
        this.slug = slug;
        this.language = language;
        this.channel = channel;
    }

    // Getters
    public String getSlug() {
        return slug;
    }

    public String getLanguage() {
        return language;
    }

    public Channel getChannel() {
        return channel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        NotificationTemplateId that = (NotificationTemplateId) o;
        return Objects.equals(slug, that.slug) &&
                Objects.equals(language, that.language) &&
                Objects.equals(channel, that.channel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(slug, language, channel);
    }
}
