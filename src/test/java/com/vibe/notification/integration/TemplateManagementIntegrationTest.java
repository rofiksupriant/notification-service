package com.vibe.notification.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.notification.application.dto.CreateTemplateRequest;
import com.vibe.notification.application.dto.UpdateTemplateRequest;
import com.vibe.notification.infrastructure.persistence.entity.NotificationTemplateId;
import com.vibe.notification.infrastructure.persistence.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Template Management Integration Tests")
class TemplateManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationTemplateRepository templateRepository;

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String VALID_API_KEY = "test-api-key";
    private static final String BASE_URL = "/api/v1/templates";

    @Test
    @DisplayName("Should complete full CRUD cycle for templates")
    void shouldCompleteFulCrudCycle() throws Exception {
        String slug = "integration-test";
        String language = "en";

        // 1. Create template
        var createRequest = new CreateTemplateRequest(
            slug,
            language,
            "EMAIL",
            "Hello [[${name}]], welcome!",
            "Welcome to Our Service",
            null,
            "TEXT"
        );

        mockMvc.perform(post(BASE_URL)
                .header(API_KEY_HEADER, VALID_API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.slug").value(slug))
            .andExpect(jsonPath("$.language").value(language))
            .andExpect(jsonPath("$.channel").value("EMAIL"));

        // 2. Verify template exists in database
        var templateId = new NotificationTemplateId(slug, language);
        assertTrue(templateRepository.findById(templateId).isPresent());

        // 3. Fetch template
        mockMvc.perform(get(BASE_URL + "/" + slug + "/" + language))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slug").value(slug))
            .andExpect(jsonPath("$.content").value("Hello [[${name}]], welcome!"))
            .andExpect(jsonPath("$.subject").value("Welcome to Our Service"));

        // 4. Update template
        var updateRequest = new UpdateTemplateRequest(
            "Updated content for [[${name}]]",
            "Updated Subject",
            null
        );

        mockMvc.perform(put(BASE_URL + "/" + slug + "/" + language)
                .header(API_KEY_HEADER, VALID_API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slug").value(slug))
            .andExpect(jsonPath("$.content").value("Updated content for [[${name}]]"))
            .andExpect(jsonPath("$.subject").value("Updated Subject"));

        // 5. Verify update in database
        var updatedTemplate = templateRepository.findById(templateId).orElseThrow();
        assertEquals("Updated content for [[${name}]]", updatedTemplate.getContent());
        assertEquals("Updated Subject", updatedTemplate.getSubject());

        // 6. Delete template
        mockMvc.perform(delete(BASE_URL + "/" + slug + "/" + language)
                .header(API_KEY_HEADER, VALID_API_KEY))
            .andExpect(status().isNoContent());

        // 7. Verify template is deleted from database
        assertTrue(templateRepository.findById(templateId).isEmpty());

        // 8. Verify fetch returns 404 after deletion
        mockMvc.perform(get(BASE_URL + "/" + slug + "/" + language))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should prevent duplicate template creation")
    void shouldPreventDuplicateTemplateCreation() throws Exception {
        String slug = "duplicate-test";
        String language = "en";

        var createRequest = new CreateTemplateRequest(
            slug,
            language,
            "EMAIL",
            "Content",
            "Subject",
            null,
            "TEXT"
        );

        // Create first template
        mockMvc.perform(post(BASE_URL)
                .header(API_KEY_HEADER, VALID_API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated());

        // Attempt to create duplicate
        mockMvc.perform(post(BASE_URL)
                .header(API_KEY_HEADER, VALID_API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should enforce API key validation on create")
    void shouldEnforceApiKeyValidationOnCreate() throws Exception {
        var createRequest = new CreateTemplateRequest(
            "test-create-validation",
            "en",
            "EMAIL",
            "Content",
            "Subject",
            null,
            "TEXT"
        );

        mockMvc.perform(post(BASE_URL)
                .header(API_KEY_HEADER, "wrong-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should enforce API key validation on update")
    void shouldEnforceApiKeyValidationOnUpdate() throws Exception {
        String slug = "api-key-validation-update-test";
        String language = "en";

        // First create a template
        var createRequest = new CreateTemplateRequest(
            slug,
            language,
            "EMAIL",
            "Content",
            "Subject",
            null,
            "TEXT"
        );

        mockMvc.perform(post(BASE_URL)
                .header(API_KEY_HEADER, VALID_API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated());

        // Attempt to update with wrong API key
        var updateRequest = new UpdateTemplateRequest("Updated", "Updated", null);
        mockMvc.perform(put(BASE_URL + "/" + slug + "/" + language)
                .header(API_KEY_HEADER, "wrong-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should enforce API key validation on delete")
    void shouldEnforceApiKeyValidationOnDelete() throws Exception {
        String slug = "api-key-validation-delete-test";
        String language = "en";

        // First create a template
        var createRequest = new CreateTemplateRequest(
            slug,
            language,
            "EMAIL",
            "Content",
            "Subject",
            null,
            "TEXT"
        );

        mockMvc.perform(post(BASE_URL)
                .header(API_KEY_HEADER, VALID_API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated());

        // Attempt to delete with wrong API key
        mockMvc.perform(delete(BASE_URL + "/" + slug + "/" + language)
                .header(API_KEY_HEADER, "wrong-key"))
            .andExpect(status().isUnauthorized());

        // Verify template still exists
        var templateId = new NotificationTemplateId(slug, language);
        assertTrue(templateRepository.findById(templateId).isPresent());
    }

    @Test
    @DisplayName("Should validate required fields on template creation")
    void shouldValidateRequiredFieldsOnTemplateCreation() throws Exception {
        // Create request with empty slug
        var invalidRequest = new CreateTemplateRequest(
            "",
            "en",
            "EMAIL",
            "Content",
            "Subject",
            null,
            "TEXT"
        );

        mockMvc.perform(post(BASE_URL)
                .header(API_KEY_HEADER, VALID_API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should support multiple templates with same slug but different languages")
    void shouldSupportMultipleLanguagesForSameSlug() throws Exception {
        String slug = "multilang-test-unique";

        // Create English version
        var enRequest = new CreateTemplateRequest(
            slug,
            "en",
            "EMAIL",
            "Hello",
            "English Subject",
            null,
            "TEXT"
        );

        mockMvc.perform(post(BASE_URL)
                .header(API_KEY_HEADER, VALID_API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(enRequest)))
            .andExpect(status().isCreated());

        // Create Indonesian version
        var idRequest = new CreateTemplateRequest(
            slug,
            "id",
            "EMAIL",
            "Halo",
            "Subject Indonesia",
            null,
            "TEXT"
        );

        mockMvc.perform(post(BASE_URL)
                .header(API_KEY_HEADER, VALID_API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(idRequest)))
            .andExpect(status().isCreated());

        // Verify both versions exist
        mockMvc.perform(get(BASE_URL + "/" + slug + "/en"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.language").value("en"))
            .andExpect(jsonPath("$.subject").value("English Subject"));

        mockMvc.perform(get(BASE_URL + "/" + slug + "/id"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.language").value("id"))
            .andExpect(jsonPath("$.subject").value("Subject Indonesia"));
    }
}
