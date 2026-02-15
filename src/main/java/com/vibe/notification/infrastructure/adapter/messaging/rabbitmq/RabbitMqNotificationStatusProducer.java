package com.vibe.notification.infrastructure.adapter.messaging.rabbitmq;

import com.vibe.notification.domain.model.NotificationStatusEvent;
import com.vibe.notification.domain.port.NotificationStatusProducer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * RabbitMQ implementation of NotificationStatusProducer.
 * Publishes notification status events to RabbitMQ with OpenTelemetry trace propagation.
 * 
 * Features:
 * - Asynchronous publishing (non-blocking)
 * - OTel trace propagation via RabbitMQ message headers
 * - Resilient to RabbitMQ failures (won't crash the application)
 * - Publishes to notification.status.exchange with routing key status.updated
 */
@Component
@ConditionalOnProperty(name = "app.feature.rabbitmq.enabled", havingValue = "true")
public class RabbitMqNotificationStatusProducer implements NotificationStatusProducer {
    
    private static final Logger logger = LoggerFactory.getLogger(RabbitMqNotificationStatusProducer.class);
    
    private final RabbitTemplate rabbitTemplate;
    private final Tracer tracer;
    private final OpenTelemetry openTelemetry;
    
    public RabbitMqNotificationStatusProducer(
            RabbitTemplate rabbitTemplate, 
            Tracer tracer,
            OpenTelemetry openTelemetry) {
        this.rabbitTemplate = rabbitTemplate;
        this.tracer = tracer;
        this.openTelemetry = openTelemetry;
    }
    
    /**
     * Publishes status event asynchronously with trace propagation.
     * Failures are logged but don't crash the application.
     */
    @Async
    @Override
    public void publishStatus(NotificationStatusEvent event) {
        try {
            logger.debug("Publishing notification status event: traceId={}, status={}, channel={}", 
                event.traceId(), event.status(), event.channel());
            
            // Create a span for this publishing operation
            Span span = tracer.spanBuilder("notification.status.publish")
                .setAttribute("trace_id", event.traceId())
                .setAttribute("status", event.status().name())
                .setAttribute("channel", event.channel().name())
                .startSpan();
            
            try {
                // Build JSON payload manually to ensure proper structure
                String jsonPayload = buildJsonPayload(event);
                
                // Create message properties and inject trace context
                MessageProperties props = new MessageProperties();
                props.setContentType("application/json");
                props.setContentEncoding("UTF-8");
                
                // Inject OpenTelemetry trace context into RabbitMQ headers
                Context currentContext = Context.current().with(span);
                openTelemetry.getPropagators().getTextMapPropagator()
                    .inject(currentContext, props, new MessagePropertiesTextMapSetter());
                
                // Also add trace_id as a custom header for easy access
                props.setHeader("trace_id", event.traceId());
                
                // Add client_id header if present
                if (event.clientId() != null && !event.clientId().isBlank()) {
                    props.setHeader("client_id", event.clientId());
                }
                
                Message message = new Message(jsonPayload.getBytes(StandardCharsets.UTF_8), props);
                
                // Build routing key with client_id if present
                String routingKey = buildRoutingKey(event.clientId());
                
                // Publish to exchange
                rabbitTemplate.send(RabbitMqConfiguration.NOTIFICATION_STATUS_EXCHANGE, 
                                  routingKey, message);
                
                logger.info("Successfully published notification status: traceId={}, status={}", 
                    event.traceId(), event.status());
            } finally {
                span.end();
            }
            
        } catch (Exception e) {
            // Log error but don't throw - status publishing must be non-blocking and safe
            logger.error("Failed to publish notification status event for traceId={}: {}", 
                event.traceId(), e.getMessage(), e);
        }
    }
    
    /**
     * Builds JSON payload from NotificationStatusEvent.
     * Uses manual JSON construction to ensure exact format.
     */
    private String buildJsonPayload(NotificationStatusEvent event) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"trace_id\":\"").append(escapeJson(event.traceId())).append("\",");
        json.append("\"status\":\"").append(event.status().name()).append("\",");
        json.append("\"channel\":\"").append(event.channel().name()).append("\",");
        
        if (event.errorMessage() != null) {
            json.append("\"error_message\":\"").append(escapeJson(event.errorMessage())).append("\",");
        } else {
            json.append("\"error_message\":null,");
        }
        
        if (event.clientId() != null && !event.clientId().isBlank()) {
            json.append("\"client_id\":\"").append(escapeJson(event.clientId())).append("\",");
        } else {
            json.append("\"client_id\":null,");
        }
        
        json.append("\"timestamp\":\"").append(event.timestamp().toString()).append("\"");
        json.append("}");
        
        return json.toString();
    }
    
    /**
     * Builds routing key with client_id if present.
     * Format: status.updated.{clientId} or status.updated if no clientId
     */
    private String buildRoutingKey(String clientId) {
        if (clientId != null && !clientId.isBlank()) {
            return RabbitMqConfiguration.NOTIFICATION_STATUS_ROUTING_KEY + "." + clientId;
        }
        return RabbitMqConfiguration.NOTIFICATION_STATUS_ROUTING_KEY;
    }
    
    /**
     * Escapes special characters in JSON strings
     */
    private String escapeJson(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
    
    /**
     * TextMapSetter for injecting trace context into RabbitMQ message properties
     */
    private static class MessagePropertiesTextMapSetter implements TextMapSetter<MessageProperties> {
        @Override
        public void set(MessageProperties carrier, String key, String value) {
            if (carrier != null) {
                carrier.setHeader(key, value);
            }
        }
    }
}
