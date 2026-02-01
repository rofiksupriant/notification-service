package com.vibe.notification.infrastructure.persistence.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Database-agnostic JPA converter for JsonNode to String.
 * Stores JSON as TEXT/VARCHAR in any database (PostgreSQL, H2, MySQL, etc).
 * Use @Convert(converter = JsonNodeConverter.class) on fields.
 */
@Converter(autoApply = false)
public class JsonNodeConverter implements AttributeConverter<JsonNode, String> {
    
    private static final Logger logger = LoggerFactory.getLogger(JsonNodeConverter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(JsonNode attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (Exception e) {
            logger.error("Failed to convert JsonNode to String", e);
            throw new RuntimeException("Failed to serialize JsonNode", e);
        }
    }

    @Override
    public JsonNode convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readTree(dbData);
        } catch (Exception e) {
            logger.error("Failed to convert String to JsonNode", e);
            throw new RuntimeException("Failed to deserialize JsonNode", e);
        }
    }
}
