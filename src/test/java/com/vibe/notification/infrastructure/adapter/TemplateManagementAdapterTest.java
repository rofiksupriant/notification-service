package com.vibe.notification.infrastructure.adapter;

import com.vibe.notification.application.dto.CreateTemplateRequest;
import com.vibe.notification.application.dto.UpdateTemplateRequest;
import com.vibe.notification.domain.exception.TemplateAlreadyExistsException;
import com.vibe.notification.domain.exception.TemplateNotFoundException;
import com.vibe.notification.domain.exception.TemplateValidationException;
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
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Template Management Adapter Tests")
class TemplateManagementAdapterTest {

    @Mock
    private NotificationTemplateRepository templateRepository;

    @Mock
    private SpringTemplateEngine templateEngine;

    @InjectMocks
    private TemplateManagementAdapter templateManagementAdapter;

    private NotificationTemplateEntity templateEntity;
    private CreateTemplateRequest createRequest;
    private UpdateTemplateRequest updateRequest;

    @BeforeEach
    void setUp() {
        // Setup EMAIL template
        var emailTemplateId = new NotificationTemplateId("welcome", "en", "email");
        templateEntity = new NotificationTemplateEntity(
            emailTemplateId,
            "TEXT",
            "Welcome to our service",
            "Hello [[${name}]], welcome!",
            null
        );
        
        // Setup WHATSAPP template
        var waTemplateId = new NotificationTemplateId("welcome", "en", "whatsapp");
        var waTemplateEntity = new NotificationTemplateEntity(
            waTemplateId,
            "TEXT",
            null,
            "Hello [[${name}]], welcome via WhatsApp!",
            null
        );

        createRequest = new CreateTemplateRequest(
            "welcome",
            "en",
            "EMAIL",
            "Hello [[${name}]], welcome!",
            "Welcome to our service",
            null,
            "TEXT"
        );

        updateRequest = new UpdateTemplateRequest(
            "Updated content [[${name}]]",
            "Updated Subject",
            null,
            "TEXT"
        );
    }

    @Test
    @DisplayName("Should create template successfully")
    void shouldCreateTemplateSuccessfully() {
        // Given
        when(templateRepository.findById(any())).thenReturn(Optional.empty());
        when(templateRepository.save(any())).thenReturn(templateEntity);

        // When
        var result = templateManagementAdapter.createTemplate(createRequest);

        // Then
        assertNotNull(result);
        assertEquals("welcome", result.slug());
        assertEquals("en", result.language());
        assertEquals("email", result.channel());
        assertEquals("TEXT", result.templateType());
        verify(templateRepository).save(any());
    }

    @Test
    @DisplayName("Should throw exception when template already exists")
    void shouldThrowExceptionWhenTemplateAlreadyExists() {
        // Given
        when(templateRepository.findById(any())).thenReturn(Optional.of(templateEntity));

        // When & Then
        assertThrows(TemplateAlreadyExistsException.class, () ->
            templateManagementAdapter.createTemplate(createRequest)
        );
        verify(templateRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw validation exception when slug is empty")
    void shouldThrowValidationExceptionWhenSlugIsEmpty() {
        // Given
        var invalidRequest = new CreateTemplateRequest(
            "",
            "en",
            "EMAIL",
            "Content",
            "Subject",
            null,
            "TEXT"
        );

        // When & Then
        assertThrows(TemplateValidationException.class, () ->
            templateManagementAdapter.createTemplate(invalidRequest)
        );
    }

    @Test
    @DisplayName("Should throw validation exception when language is empty")
    void shouldThrowValidationExceptionWhenLanguageIsEmpty() {
        // Given
        var invalidRequest = new CreateTemplateRequest(
            "welcome",
            "",
            "EMAIL",
            "Content",
            "Subject",
            null,
            "TEXT"
        );

        // When & Then
        assertThrows(TemplateValidationException.class, () ->
            templateManagementAdapter.createTemplate(invalidRequest)
        );
    }

    @Test
    @DisplayName("Should throw validation exception when channel is empty")
    void shouldThrowValidationExceptionWhenChannelIsEmpty() {
        // Given
        var invalidRequest = new CreateTemplateRequest(
            "welcome",
            "en",
            "",
            "Content",
            "Subject",
            null,
            "TEXT"
        );

        // When & Then
        assertThrows(TemplateValidationException.class, () ->
            templateManagementAdapter.createTemplate(invalidRequest)
        );
    }

    @Test
    @DisplayName("Should throw validation exception when content is empty")
    void shouldThrowValidationExceptionWhenContentIsEmpty() {
        // Given
        var invalidRequest = new CreateTemplateRequest(
            "welcome",
            "en",
            "EMAIL",
            "",
            "Subject",
            null,
            "TEXT"
        );

        // When & Then
        assertThrows(TemplateValidationException.class, () ->
            templateManagementAdapter.createTemplate(invalidRequest)
        );
    }

    @Test
    @DisplayName("Should fetch template successfully")
    void shouldFetchTemplateSuccessfully() {
        // Given
        var templateId = new NotificationTemplateId("welcome", "en", "email");
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(templateEntity));

        // When
        var result = templateManagementAdapter.getTemplate("welcome", "en", "email");

        // Then
        assertNotNull(result);
        assertEquals("welcome", result.slug());
        assertEquals("en", result.language());
        assertEquals("email", result.channel());
        verify(templateRepository).findById(templateId);
    }

    @Test
    @DisplayName("Should throw TemplateNotFoundException when template not found")
    void shouldThrowTemplateNotFoundExceptionWhenTemplateNotFound() {
        // Given
        var templateId = new NotificationTemplateId("missing", "en", "email");
        when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(TemplateNotFoundException.class, () ->
            templateManagementAdapter.getTemplate("missing", "en", "email")
        );
    }

    @Test
    @DisplayName("Should throw validation exception when slug is empty in get")
    void shouldThrowValidationExceptionWhenSlugIsEmptyInGet() {
        // When & Then
        assertThrows(TemplateValidationException.class, () ->
            templateManagementAdapter.getTemplate("", "en", "email")
        );
    }

    @Test
    @DisplayName("Should update template successfully")
    void shouldUpdateTemplateSuccessfully() {
        // Given
        var templateId = new NotificationTemplateId("welcome", "en", "email");
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(templateEntity));
        when(templateRepository.save(any())).thenReturn(templateEntity);

        // When
        var result = templateManagementAdapter.updateTemplate("welcome", "en", "email", updateRequest);

        // Then
        assertNotNull(result);
        assertEquals("welcome", result.slug());
        assertEquals("en", result.language());
        verify(templateRepository).findById(templateId);
        verify(templateRepository).save(any());
    }

    @Test
    @DisplayName("Should throw TemplateNotFoundException when updating non-existent template")
    void shouldThrowTemplateNotFoundWhenUpdatingNonExistentTemplate() {
        // Given
        var templateId = new NotificationTemplateId("missing", "en", "email");
        when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(TemplateNotFoundException.class, () ->
            templateManagementAdapter.updateTemplate("missing", "en", "email", updateRequest)
        );
    }

    @Test
    @DisplayName("Should throw validation exception when content is empty in update")
    void shouldThrowValidationExceptionWhenContentIsEmptyInUpdate() {
        // Given
        var invalidRequest = new UpdateTemplateRequest("", "Subject", null, "TEXT");

        // When & Then
        assertThrows(TemplateValidationException.class, () ->
            templateManagementAdapter.updateTemplate("welcome", "en", "email", invalidRequest)
        );
    }

    @Test
    @DisplayName("Should delete template successfully")
    void shouldDeleteTemplateSuccessfully() {
        // Given
        var templateId = new NotificationTemplateId("welcome", "en", "email");
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(templateEntity));

        // When
        templateManagementAdapter.deleteTemplate("welcome", "en", "email");

        // Then
        verify(templateRepository).findById(templateId);
        verify(templateRepository).delete(templateEntity);
    }

    @Test
    @DisplayName("Should throw TemplateNotFoundException when deleting non-existent template")
    void shouldThrowTemplateNotFoundWhenDeletingNonExistentTemplate() {
        // Given
        var templateId = new NotificationTemplateId("missing", "en", "email");
        when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(TemplateNotFoundException.class, () ->
            templateManagementAdapter.deleteTemplate("missing", "en", "email")
        );
    }
}
