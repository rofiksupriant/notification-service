package com.vibe.notification.infrastructure.persistence.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JPA converter to handle JsonNode serialization/deserialization
 * Works with both PostgreSQL JSONB and H2 TEXT columns
 */
@Converter(autoApply = true)
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
