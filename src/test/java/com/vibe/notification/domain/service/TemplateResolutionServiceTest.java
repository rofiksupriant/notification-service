package com.vibe.notification.domain.service;

import com.vibe.notification.domain.dto.TemplateDTO;
import com.vibe.notification.domain.dto.TemplateIdDTO;
import com.vibe.notification.domain.exception.TemplateNotFoundException;
import com.vibe.notification.domain.port.NotificationTemplatePort;
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
    private NotificationTemplatePort templatePort;

    @InjectMocks
    private TemplateResolutionService templateResolutionService;

    private TemplateDTO idTemplate;
    private TemplateDTO enTemplate;
    private TemplateDTO idWaTemplate;
    private TemplateDTO enWaTemplate;

    @BeforeEach
    void setUp() {
        // Create Indonesian EMAIL template
        idTemplate = TemplateTestHelper.createTemplateDTO(
            "welcome", "id", "EMAIL",
            "Selamat Datang",
            "Halo [[${name}]], selamat datang di layanan kami!",
            null
        );

        // Create English EMAIL template (fallback)
        enTemplate = TemplateTestHelper.createTemplateDTO(
            "welcome", "en", "EMAIL",
            "Welcome",
            "Hello [[${name}]], welcome to our service!",
            null
        );
        
        // Create Indonesian WHATSAPP template
        idWaTemplate = TemplateTestHelper.createTemplateDTO(
            "welcome", "id", "WHATSAPP",
            null,
            "Halo [[${name}]], selamat datang di layanan kami via WhatsApp!",
            null
        );
        
        // Create English WHATSAPP template (fallback)
        enWaTemplate = TemplateTestHelper.createTemplateDTO(
            "welcome", "en", "WHATSAPP",
            null,
            "Hello [[${name}]], welcome to our service via WhatsApp!",
            null
        );
    }

    @Test
    @DisplayName("Should resolve template when requested language exists")
    void shouldResolveTemplateInRequestedLanguage() {
        // Given
        var templateId = new TemplateIdDTO("welcome", "id", "email");
        when(templatePort.findById(templateId))
            .thenReturn(Optional.of(idTemplate));

        // When
        var result = templateResolutionService.resolveTemplate("welcome", "id", "email");

        // Then
        assertNotNull(result);
        assertEquals("id", result.getId().getLanguage());
        assertEquals("Selamat Datang", result.getSubject());
    }

    @Test
    @DisplayName("Should fallback to English when requested language not found")
    void shouldFallbackToEnglish() {
        // Given
        var frenchId = new TemplateIdDTO("welcome", "fr", "email");
        var englishId = new TemplateIdDTO("welcome", "en", "email");
        
        when(templatePort.findById(frenchId))
            .thenReturn(Optional.empty());
        when(templatePort.findById(englishId))
            .thenReturn(Optional.of(enTemplate));

        // When
        var result = templateResolutionService.resolveTemplate("welcome", "fr", "email");

        // Then
        assertNotNull(result);
        assertEquals("en", result.getId().getLanguage());
        assertEquals("Welcome", result.getSubject());
    }

    @Test
    @DisplayName("Should throw exception when template not found in any language")
    void shouldThrowExceptionWhenTemplateNotFound() {
        // Given
        var missingId = new TemplateIdDTO("missing", "de", "email");
        when(templatePort.findById(missingId))
            .thenReturn(Optional.empty());
        
        var englishId = new TemplateIdDTO("missing", "en", "email");
        when(templatePort.findById(englishId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(TemplateNotFoundException.class, () ->
            templateResolutionService.resolveTemplate("missing", "de", "email")
        );
    }
}
