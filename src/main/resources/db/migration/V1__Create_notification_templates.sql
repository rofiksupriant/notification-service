CREATE TABLE IF NOT EXISTS notification_templates (
    slug VARCHAR(50) NOT NULL,
    language VARCHAR(5) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    template_type VARCHAR(20) NOT NULL,
    subject VARCHAR(255),
    content TEXT NOT NULL,
    image_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (slug, language)
);

CREATE INDEX IF NOT EXISTS idx_templates_channel ON notification_templates(channel);
CREATE INDEX IF NOT EXISTS idx_templates_type ON notification_templates(template_type);
