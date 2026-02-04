package com.vibe.notification.domain.port;

import com.vibe.notification.domain.model.NotificationStatusEvent;

/**
 * Domain port for publishing notification status events.
 * Following DDD pattern: interface defined in domain layer, implemented in infrastructure layer.
 * 
 * This port enables the domain layer to publish notification status updates without
 * knowing the underlying messaging infrastructure (RabbitMQ).
 */
public interface NotificationStatusProducer {
    
    /**
     * Publish a notification status event asynchronously.
     * This method must be non-blocking and resilient to messaging failures.
     * 
     * @param event the notification status event to publish
     */
    void publishStatus(NotificationStatusEvent event);
}
