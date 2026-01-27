package com.vibe.notification.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * OpenAPI Configuration for Swagger UI
 * Configures comprehensive API documentation with API Key authentication,
 * detailed schemas, and complete endpoint specifications.
 */
@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Notification Service API")
                        .version("1.0.0")
                        .description("High-performance asynchronous notification service for WhatsApp and Email. " +
                                "This API provides comprehensive endpoints for managing notification templates and sending notifications " +
                                "across multiple channels including WhatsApp and Email.")
                        .termsOfService("https://www.vibe.com/terms")
                        .contact(new Contact()
                                .name("Vibe Support Team")
                                .email("support@vibe.com")
                                .url("https://www.vibe.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(Arrays.asList(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development Environment"),
                        new Server()
                                .url("https://api.notification.vibe.com")
                                .description("Production Environment"),
                        new Server()
                                .url("https://staging-api.notification.vibe.com")
                                .description("Staging Environment")))
                .components(new Components()
                        .addSecuritySchemes("X-API-Key", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-API-Key")
                                .description("API Key for accessing secured endpoints. Include this header in all requests to protected endpoints (POST, PUT, DELETE). " +
                                        "The API key must be a valid string issued by the Vibe platform. Contact support for API key provisioning.")))
                .tags(Arrays.asList(
                        new Tag()
                                .name("Template Management")
                                .description("Comprehensive APIs for managing notification templates. Supports CRUD operations for templates with multi-language support. " +
                                        "All write operations require API Key authentication."),
                        new Tag()
                                .name("Notification Engine")
                                .description("APIs for sending notifications through configured channels (WhatsApp, Email). Supports template-based notifications with variable interpolation.")));
    }
}
