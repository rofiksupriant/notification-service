package com.vibe.notification.infrastructure.adapter.messaging.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.notification.domain.model.Channel;
import com.vibe.notification.domain.model.NotificationStatusEvent;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RabbitMqNotificationStatusProducer.
 */
@ExtendWith(MockitoExtension.class)
class RabbitMqNotificationStatusProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private Tracer tracer;

    @Mock
    private OpenTelemetry openTelemetry;

    @Mock
    private ContextPropagators contextPropagators;

    @Mock
    private TextMapPropagator textMapPropagator;

    @Mock
    private SpanBuilder spanBuilder;

    @Mock
    private Span span;

    private RabbitMqNotificationStatusProducer statusProducer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        
        // Set up OpenTelemetry mocks
        when(openTelemetry.getPropagators()).thenReturn(contextPropagators);
        when(contextPropagators.getTextMapPropagator()).thenReturn(textMapPropagator);
        when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        when(spanBuilder.setAttribute(anyString(), anyString())).thenReturn(spanBuilder);
        when(spanBuilder.startSpan()).thenReturn(span);
        
        statusProducer = new RabbitMqNotificationStatusProducer(rabbitTemplate, tracer, openTelemetry);
    }

    @Test
    void testPublishSuccessStatus() throws Exception {
        String traceId = "test-trace-id-123";
        NotificationStatusEvent event = NotificationStatusEvent.success(traceId, Channel.EMAIL, null);

        statusProducer.publishStatus(event);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate, timeout(1000).times(1)).send(
            eq(RabbitMqConfiguration.NOTIFICATION_STATUS_EXCHANGE),
            eq(RabbitMqConfiguration.NOTIFICATION_STATUS_ROUTING_KEY),
            messageCaptor.capture()
        );

        Message capturedMessage = messageCaptor.getValue();
        String jsonPayload = new String(capturedMessage.getBody(), "UTF-8");

        assertTrue(jsonPayload.contains("\"trace_id\":\"" + traceId + "\""));
        assertTrue(jsonPayload.contains("\"status\":\"SUCCESS\""));
        assertTrue(jsonPayload.contains("\"channel\":\"EMAIL\""));
        assertTrue(jsonPayload.contains("\"error_message\":null"));
        assertTrue(jsonPayload.contains("\"client_id\":null"));
    }

    @Test
    void testPublishFailedStatus() throws Exception {
        String traceId = "test-trace-id-456";
        String errorMessage = "SMTP connection failed";
        NotificationStatusEvent event = NotificationStatusEvent.failure(traceId, Channel.EMAIL, errorMessage, null);

        statusProducer.publishStatus(event);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate, timeout(1000).times(1)).send(
            eq(RabbitMqConfiguration.NOTIFICATION_STATUS_EXCHANGE),
            eq(RabbitMqConfiguration.NOTIFICATION_STATUS_ROUTING_KEY),
            messageCaptor.capture()
        );

        Message capturedMessage = messageCaptor.getValue();
        String jsonPayload = new String(capturedMessage.getBody(), "UTF-8");

        assertTrue(jsonPayload.contains("\"trace_id\":\"" + traceId + "\""));
        assertTrue(jsonPayload.contains("\"status\":\"FAILED\""));
        assertTrue(jsonPayload.contains("\"error_message\":\"" + errorMessage + "\""));
    }

    @Test
    void testPublishStatusDoesNotThrowOnRabbitMQError() {
        String traceId = "test-trace-id-error";
        NotificationStatusEvent event = NotificationStatusEvent.success(traceId, Channel.EMAIL, null);
        
        doThrow(new RuntimeException("RabbitMQ connection failed"))
            .when(rabbitTemplate).send(anyString(), anyString(), any(Message.class));

        assertDoesNotThrow(() -> statusProducer.publishStatus(event));
        
        // Verify the call was attempted
        verify(rabbitTemplate, timeout(1000).atLeastOnce()).send(anyString(), anyString(), any(Message.class));
    }
    
    @Test
    void testPublishStatusWithClientId() throws Exception {
        String traceId = "test-trace-id-789";
        String clientId = "client-abc-123";
        NotificationStatusEvent event = NotificationStatusEvent.success(traceId, Channel.EMAIL, clientId);

        statusProducer.publishStatus(event);

        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate, timeout(1000).times(1)).send(
            eq(RabbitMqConfiguration.NOTIFICATION_STATUS_EXCHANGE),
            routingKeyCaptor.capture(),
            messageCaptor.capture()
        );

        // Verify routing key includes client_id
        String capturedRoutingKey = routingKeyCaptor.getValue();
        assertEquals(RabbitMqConfiguration.NOTIFICATION_STATUS_ROUTING_KEY + "." + clientId, capturedRoutingKey);

        // Verify JSON payload includes client_id
        Message capturedMessage = messageCaptor.getValue();
        String jsonPayload = new String(capturedMessage.getBody(), "UTF-8");
        assertTrue(jsonPayload.contains("\"client_id\":\"" + clientId + "\""));
        
        // Verify headers include client_id
        Object clientIdHeader = capturedMessage.getMessageProperties().getHeader("client_id");
        assertEquals(clientId, clientIdHeader);
    }
}
