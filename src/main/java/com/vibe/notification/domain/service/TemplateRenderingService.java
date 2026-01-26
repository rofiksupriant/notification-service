package com.vibe.notification.domain.service;

import com.vibe.notification.domain.exception.TemplateRenderingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

/**
 * Service for rendering Thymeleaf templates with variable substitution
 */
@Service
public class TemplateRenderingService {
    private static final Logger logger = LoggerFactory.getLogger(TemplateRenderingService.class);

    private final ITemplateEngine templateEngine;

    public TemplateRenderingService(ITemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    /**
     * Render template content with variables
     * Template content uses Thymeleaf syntax: [[${var}]]
     */
    public String renderContent(String templateContent, Map<String, Object> variables) {
        try {
            logger.debug("Rendering template content with {} variables", 
                variables != null ? variables.size() : 0);
            
            Context context = new Context();
            if (variables != null) {
                context.setVariables(variables);
            }

            // Use StringTemplateResolver for DB-based templates
            String rendered = templateEngine.process(templateContent, context);
            logger.debug("Template rendered successfully");
            return rendered;
        } catch (Exception e) {
            logger.error("Template rendering failed", e);
            throw new TemplateRenderingException("Failed to render template", e);
        }
    }

    /**
     * Render template subject (optional, for email)
     */
    public String renderSubject(String subject, Map<String, Object> variables) {
        if (subject == null || subject.isBlank()) {
            return null;
        }
        return renderContent(subject, variables);
    }
}
