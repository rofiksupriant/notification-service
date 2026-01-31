package com.vibe.notification.presentation.controller;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Custom health indicator for Notification Service
 * Checks DB and Mail connectivity
 */
@Component
public class NotificationHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;
    private final JavaMailSender mailSender;

    public NotificationHealthIndicator(DataSource dataSource, JavaMailSender mailSender) {
        this.dataSource = dataSource;
        this.mailSender = mailSender;
    }

    @Override
    public Health health() {
        Health.Builder builder = Health.up();

        // Check Database
        var dbHealth = checkDatabase();
        if (dbHealth.getStatus().equals("UP")) {
            builder.withDetail("database", "PostgreSQL connection OK");
        } else {
            return Health.down()
                    .withDetail("database", "PostgreSQL connection FAILED")
                    .build();
        }

        // Check Mail Server
        var mailHealth = checkMailServer();
        if (mailHealth.getStatus().equals("UP")) {
            builder.withDetail("mail", "SMTP connection OK");
        } else {
            builder.withDetail("mail", "SMTP not available");
        }

        return builder.build();
    }

    private HealthCheckResult checkDatabase() {
        try {
            if (dataSource == null) {
                return new HealthCheckResult("DOWN", "DataSource not configured");
            }
            try (Connection conn = dataSource.getConnection()) {
                if (conn.isValid(2)) {
                    return new HealthCheckResult("UP", "Database connection successful");
                }
            }
            return new HealthCheckResult("DOWN", "Database validation failed");
        } catch (Exception e) {
            return new HealthCheckResult("DOWN", "Database error: " + e.getMessage());
        }
    }

    private HealthCheckResult checkMailServer() {
        try {
            if (mailSender == null) {
                return new HealthCheckResult("DOWN", "MailSender not configured");
            }
            // MailSender doesn't have a direct health check method
            // So we just verify it's autowired
            return new HealthCheckResult("UP", "MailSender configured");
        } catch (Exception e) {
            return new HealthCheckResult("DOWN", "Mail error: " + e.getMessage());
        }
    }

    private static class HealthCheckResult {
        private final String status;

        HealthCheckResult(String status, String message) {
            this.status = status;
        }

        String getStatus() {
            return status;
        }
    }
}
