package com.vibe.notification.presentation.controller;

import com.vibe.notification.application.NotificationApplicationService;
import com.vibe.notification.application.dto.SendNotificationRequest;
import com.vibe.notification.application.dto.NotificationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Notification API
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationApplicationService notificationApplicationService;

    public NotificationController(NotificationApplicationService notificationApplicationService) {
        this.notificationApplicationService = notificationApplicationService;
    }

    /**
     * Send notification endpoint
     * POST /api/v1/notifications/send
     */
    @PostMapping("/send")
    public ResponseEntity<NotificationResponse> sendNotification(@RequestBody SendNotificationRequest request) {
        logger.info("Received notification request: {}", request.recipient());
        var response = notificationApplicationService.sendNotification(request);
        return ResponseEntity.accepted().body(response);
    }

    /**
     * Health check endpoint
     * GET /api/v1/notifications/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Notification Service is healthy");
    }
}
