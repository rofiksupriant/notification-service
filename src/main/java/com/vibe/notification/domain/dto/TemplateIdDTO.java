package com.vibe.notification.domain.dto;

import java.io.Serializable;

/**
 * Domain Data Transfer Object for template ID (composite key)
 */
public class TemplateIdDTO implements Serializable {
    private final String slug;
    private final String language;
    private final String channel;

    public TemplateIdDTO(String slug, String language, String channel) {
        this.slug = slug;
        this.language = language;
        this.channel = channel;
    }

    public String getSlug() {
        return slug;
    }

    public String getLanguage() {
        return language;
    }

    public String getChannel() {
        return channel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TemplateIdDTO that = (TemplateIdDTO) o;

        if (!slug.equals(that.slug)) return false;
        if (!language.equals(that.language)) return false;
        return channel.equals(that.channel);
    }

    @Override
    public int hashCode() {
        int result = slug.hashCode();
        result = 31 * result + language.hashCode();
        result = 31 * result + channel.hashCode();
        return result;
    }
}
