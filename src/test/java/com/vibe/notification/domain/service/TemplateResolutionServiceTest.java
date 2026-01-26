package com.vibe.notification.domain.service;

import com.vibe.notification.domain.exception.TemplateNotFoundException;
import com.vibe.notification.infrastructure.persistence.entity.NotificationTemplateEntity;
import com.vibe.notification.infrastructure.persistence.entity.NotificationTemplateId;
import com.vibe.notification.infrastructure.persistence.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Template Resolution Service - Language Fallback Tests")
class TemplateResolutionServiceTest {

    @Mock
    private NotificationTemplateRepository templateRepository;

    @InjectMocks
    private TemplateResolutionService templateResolutionService;

    private NotificationTemplateEntity idTemplate;
    private NotificationTemplateEntity enTemplate;

    @BeforeEach
    void setUp() {
        // Create Indonesian template
        idTemplate = new NotificationTemplateEntity(
            new NotificationTemplateId("welcome", "id"),
            "EMAIL",
            "TEXT",
            "Selamat Datang",
            "Halo [[${name}]], selamat datang di layanan kami!",
            null
        );

        // Create English template (fallback)
        enTemplate = new NotificationTemplateEntity(
            new NotificationTemplateId("welcome", "en"),
            "EMAIL",
            "TEXT",
            "Welcome",
            "Hello [[${name}]], welcome to our service!",
            null
        );
    }

    @Test
    @DisplayName("Should resolve template when requested language exists")
    void shouldResolveTemplateInRequestedLanguage() {
        // Given
        var templateId = new NotificationTemplateId("welcome", "id");
        when(templateRepository.findById(templateId))
            .thenReturn(Optional.of(idTemplate));

        // When
        var result = templateResolutionService.resolveTemplate("welcome", "id");

        // Then
        assertNotNull(result);
        assertEquals("id", result.getId().getLanguage());
        assertEquals("Selamat Datang", result.getSubject());
    }

    @Test
    @DisplayName("Should fallback to English when requested language not found")
    void shouldFallbackToEnglish() {
        // Given
        var frenchId = new NotificationTemplateId("welcome", "fr");
        var englishId = new NotificationTemplateId("welcome", "en");
        
        when(templateRepository.findById(frenchId))
            .thenReturn(Optional.empty());
        when(templateRepository.findById(englishId))
            .thenReturn(Optional.of(enTemplate));

        // When
        var result = templateResolutionService.resolveTemplate("welcome", "fr");

        // Then
        assertNotNull(result);
        assertEquals("en", result.getId().getLanguage());
        assertEquals("Welcome", result.getSubject());
    }

    @Test
    @DisplayName("Should throw exception when template not found in any language")
    void shouldThrowExceptionWhenTemplateNotFound() {
        // Given
        var missingId = new NotificationTemplateId("missing", "de");
        when(templateRepository.findById(missingId))
            .thenReturn(Optional.empty());
        
        var englishId = new NotificationTemplateId("missing", "en");
        when(templateRepository.findById(englishId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(TemplateNotFoundException.class, () ->
            templateResolutionService.resolveTemplate("missing", "de")
        );
    }
}
