package com.vibe.notification.integration;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Abstract base class for integration tests using containerized PostgreSQL and RabbitMQ.
 * 
 * All integration tests should extend this class to ensure they:
 * 1. Run against real PostgreSQL (not H2 emulation)
 * 2. Test JSONB columns correctly
 * 3. Catch production infrastructure compatibility issues early
 * 
 * The containerized services are automatically:
 * - Started before test class execution
 * - Available with automatic port mapping
 * - Stopped after test class execution
 * 
 * Note: Flyway is disabled for tests. Schema is managed by Hibernate (ddl-auto:
 * create-drop). RabbitMQ is enabled for all tests to ensure single shared Spring context.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "app.feature.rabbitmq.enabled=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractPostgresIntegrationTest {

    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("notif_db")
            .withUsername("postgres")
            .withPassword("postgres")
            .withReuse(true);
    
    @SuppressWarnings("resource")
    static final RabbitMQContainer rabbitmq = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.12-management-alpine"))
            .waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1))
            .withReuse(true);
    
    static {
        postgres.start();
        rabbitmq.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Configure PostgreSQL connection
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        
        // Configure RabbitMQ connection
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitmq::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitmq::getAdminPassword);
    }
}
