package com.vibe.notification.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.ITemplateEngine;
import java.util.HashMap;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Template Rendering Service Tests")
class TemplateRenderingServiceTest {

    private TemplateRenderingService templateRenderingService;

    @Mock
    private ITemplateEngine templateEngine;

    @BeforeEach
    void setUp() {
        // Use real Thymeleaf engine for testing
        var realEngine = new org.thymeleaf.TemplateEngine();
        realEngine.addTemplateResolver(new org.thymeleaf.templateresolver.StringTemplateResolver());
        templateRenderingService = new TemplateRenderingService(realEngine);
    }

    @Test
    @DisplayName("Should render template with variables")
    void shouldRenderTemplateWithVariables() {
        // Given
        var template = "Hello [[${name}]], your balance is [[${balance}]]";
        var variables = new HashMap<String, Object>();
        variables.put("name", "John");
        variables.put("balance", "Rp 500.000");

        // When
        var result = templateRenderingService.renderContent(template, variables);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("John"));
        assertTrue(result.contains("500.000"));
    }

    @Test
    @DisplayName("Should handle null variables gracefully")
    void shouldHandleNullVariables() {
        // Given
        var template = "Welcome to our service";

        // When
        var result = templateRenderingService.renderContent(template, null);

        // Then
        assertNotNull(result);
        assertEquals(template, result);
    }

    @Test
    @DisplayName("Should render subject with variables")
    void shouldRenderSubjectWithVariables() {
        // Given
        var subject = "Order confirmation for [[${orderId}]]";
        var variables = new HashMap<String, Object>();
        variables.put("orderId", "ORD-12345");

        // When
        var result = templateRenderingService.renderSubject(subject, variables);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("ORD-12345"));
    }

    @Test
    @DisplayName("Should handle null or blank subject")
    void shouldHandleNullSubject() {
        // When & Then
        assertNull(templateRenderingService.renderSubject(null, null));
        assertNull(templateRenderingService.renderSubject("", null));
    }
}
