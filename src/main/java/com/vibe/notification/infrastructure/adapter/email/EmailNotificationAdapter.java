package com.vibe.notification.infrastructure.adapter.email;

import com.vibe.notification.domain.exception.NotificationException;
import com.vibe.notification.infrastructure.persistence.entity.NotificationTemplateEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Email notification adapter using JavaMailSender
 */
@Component
public class EmailNotificationAdapter {
    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationAdapter.class);

    private final JavaMailSender mailSender;
    private final EmailProperties emailProperties;

    public EmailNotificationAdapter(JavaMailSender mailSender, EmailProperties emailProperties) {
        this.mailSender = mailSender;
        this.emailProperties = emailProperties;
    }

    /**
     * Send email notification
     */
    public void sendEmail(String recipient, NotificationTemplateEntity template, String renderedContent, String renderedSubject) {
        try {
            logger.debug("Sending email to: {}", recipient);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailProperties.getFromAddress());
            message.setTo(recipient);
            message.setSubject(renderedSubject != null ? renderedSubject : "[No Subject]");
            message.setText(renderedContent);

            mailSender.send(message);
            logger.info("Email sent successfully to: {}", recipient);
        } catch (Exception e) {
            logger.error("Failed to send email to {}: {}", recipient, e.getMessage());
            throw new NotificationException("Failed to send email: " + e.getMessage(), e);
        }
    }
}
