package com.vibe.notification.integration;

/**
 * Abstract base class for integration tests that require RabbitMQ.
 * 
 * Extends AbstractPostgresIntegrationTest which now includes both PostgreSQL and RabbitMQ containers.
 * 
 * Use this for tests that need to verify:
 * - Message queue processing
 * - RabbitMQ listener behavior
 * - Retry and DLQ mechanisms
 * - End-to-end async notification flow
 * 
 * Note: All containers are now managed in AbstractPostgresIntegrationTest to ensure
 * a single shared Spring context across all integration tests.
 */
public abstract class AbstractRabbitMqIntegrationTest extends AbstractPostgresIntegrationTest {
    // Containers inherited from parent class
}
