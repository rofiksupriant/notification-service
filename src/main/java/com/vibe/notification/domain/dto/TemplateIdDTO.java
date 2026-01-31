package com.vibe.notification.domain.dto;

import com.vibe.notification.domain.model.Channel;
import java.io.Serializable;

/**
 * Domain Data Transfer Object for template ID (composite key)
 */
public class TemplateIdDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String slug;
    private final String language;
    private final Channel channel;

    public TemplateIdDTO(String slug, String language, Channel channel) {
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

    public Channel getChannel() {
        return channel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        TemplateIdDTO that = (TemplateIdDTO) o;

        if (!slug.equals(that.slug))
            return false;
        if (!language.equals(that.language))
            return false;
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
