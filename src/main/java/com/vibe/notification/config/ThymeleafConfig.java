package com.vibe.notification.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;

/**
 * Thymeleaf configuration for String Template Resolver (DB-based templates)
 */
@Configuration
public class ThymeleafConfig {

    @Autowired
    public void configureThymeleaf(SpringTemplateEngine templateEngine) {
        var stringResolver = new StringTemplateResolver();
        stringResolver.setOrder(1);
        stringResolver.setCheckExistence(false);
        templateEngine.addTemplateResolver(stringResolver);
    }
}
