package com.vibe.notification.domain.service;

import com.vibe.notification.domain.dto.TemplateDTO;
import com.vibe.notification.domain.dto.TemplateIdDTO;
import com.vibe.notification.domain.exception.TemplateNotFoundException;
import com.vibe.notification.domain.model.Channel;
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

    @BeforeEach
    void setUp() {
        // Create Indonesian EMAIL template
        idTemplate = TemplateTestHelper.createTemplateDTO(
                "welcome", "id", Channel.EMAIL,
                "Selamat Datang",
                "Halo [[${name}]], selamat datang di layanan kami!",
                null);

        // Create English EMAIL template (fallback)
        enTemplate = TemplateTestHelper.createTemplateDTO(
                "welcome", "en", Channel.EMAIL,
                "Welcome",
                "Hello [[${name}]], welcome to our service!",
                null);

    }

    @Test
    @DisplayName("Should resolve template when requested language exists")
    void shouldResolveTemplateInRequestedLanguage() {
        // Given
        var templateId = new TemplateIdDTO("welcome", "id", Channel.EMAIL);
        when(templatePort.findById(templateId))
                .thenReturn(Optional.of(idTemplate));

        // When
        var result = templateResolutionService.resolveTemplate("welcome", "id", Channel.EMAIL);

        // Then
        assertNotNull(result);
        assertEquals("id", result.getId().getLanguage());
        assertEquals("Selamat Datang", result.getSubject());
    }

    @Test
    @DisplayName("Should fallback to English when requested language not found")
    void shouldFallbackToEnglish() {
        // Given
        var frenchId = new TemplateIdDTO("welcome", "fr", Channel.EMAIL);
        var englishId = new TemplateIdDTO("welcome", "en", Channel.EMAIL);

        when(templatePort.findById(frenchId))
                .thenReturn(Optional.empty());
        when(templatePort.findById(englishId))
                .thenReturn(Optional.of(enTemplate));

        // When
        var result = templateResolutionService.resolveTemplate("welcome", "fr", Channel.EMAIL);

        // Then
        assertNotNull(result);
        assertEquals("en", result.getId().getLanguage());
        assertEquals("Welcome", result.getSubject());
    }

    @Test
    @DisplayName("Should throw exception when template not found in any language")
    void shouldThrowExceptionWhenTemplateNotFound() {
        // Given
        var missingId = new TemplateIdDTO("missing", "de", Channel.EMAIL);
        when(templatePort.findById(missingId))
                .thenReturn(Optional.empty());

        var englishId = new TemplateIdDTO("missing", "en", Channel.EMAIL);
        when(templatePort.findById(englishId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(TemplateNotFoundException.class,
                () -> templateResolutionService.resolveTemplate("missing", "de", Channel.EMAIL));
    }
}
