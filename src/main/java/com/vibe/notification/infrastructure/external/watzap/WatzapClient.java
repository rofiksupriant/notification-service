package com.vibe.notification.infrastructure.external.watzap;

import com.vibe.notification.domain.exception.NotificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Watzap.id API client with timeout handling
 * Supports sending text messages and images
 */
@Component
public class WatzapClient {
    private static final Logger logger = LoggerFactory.getLogger(WatzapClient.class);

    private final WebClient webClient;
    private final WatzapProperties watzapProperties;

    public WatzapClient(WebClient.Builder webClientBuilder, WatzapProperties watzapProperties) {
        this.watzapProperties = watzapProperties;
        this.webClient = webClientBuilder
            .baseUrl(watzapProperties.getBaseUrl())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    /**
     * Send text message to WhatsApp recipient
     */
    public WatzapResponse sendTextMessage(String phoneNumber, String message) {
        logger.debug("Sending text message to {}", phoneNumber);

        var requestBody = new HashMap<String, Object>();
        requestBody.put("number", phoneNumber);
        requestBody.put("text", message);

        return sendRequest("/send_message", requestBody);
    }

    /**
     * Send image message to WhatsApp recipient
     */
    public WatzapResponse sendImageMessage(String phoneNumber, String imageUrl, String caption) {
        logger.debug("Sending image message to {}", phoneNumber);

        var requestBody = new HashMap<String, Object>();
        requestBody.put("number", phoneNumber);
        requestBody.put("image", imageUrl);
        if (caption != null && !caption.isBlank()) {
            requestBody.put("caption", caption);
        }

        return sendRequest("/send_image", requestBody);
    }

    /**
     * Generic request sender with timeout and retry logic
     */
    private WatzapResponse sendRequest(String endpoint, Map<String, Object> requestBody) {
        try {
            return webClient.post()
                .uri(endpoint)
                .header("Authorization", "Bearer " + watzapProperties.getApiKey())
                .header("X-Number-Key", watzapProperties.getNumberKey())
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(WatzapResponse.class)
                .timeout(Duration.ofMillis(watzapProperties.getTimeout().getReadMs()))
                .retryWhen(Retry.backoff(2, Duration.ofMillis(500)))
                .block();
        } catch (WebClientResponseException e) {
            logger.error("Watzap API error: status={}, message={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new NotificationException("Watzap API error: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Failed to send request to Watzap API", e);
            throw new NotificationException("Watzap API call failed: " + e.getMessage(), e);
        }
    }
}
