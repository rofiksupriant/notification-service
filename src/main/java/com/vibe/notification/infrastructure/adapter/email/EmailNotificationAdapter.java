package com.vibe.notification.infrastructure.adapter.email;

import com.vibe.notification.application.port.EmailNotificationPort;
import com.vibe.notification.domain.dto.TemplateDTO;
import com.vibe.notification.domain.exception.NotificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Email notification adapter implementing EmailNotificationPort
 */
@Component
public class EmailNotificationAdapter implements EmailNotificationPort {
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
    @Override
    public void sendEmail(String recipient, TemplateDTO template, String renderedSubject, String renderedContent) {
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
            throw new NotificationException("Failed to send email: " + e.getMessage(), e);
        }
    }
}
