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
     * API: https://api.watzap.id/v1/send_message
     */
    public WatzapResponse sendTextMessage(String phoneNumber, String message) {
        logger.debug("Sending text message to {}", phoneNumber);

        var requestBody = new HashMap<String, Object>();
        requestBody.put("api_key", watzapProperties.getApiKey());
        requestBody.put("number_key", watzapProperties.getNumberKey());
        requestBody.put("phone_no", phoneNumber);
        requestBody.put("message", message);
        requestBody.put("wait_until_send", "1");

        return sendRequest("/send_message", requestBody);
    }

    /**
     * Send image message to WhatsApp recipient
     * API: https://api.watzap.id/v1/send_message
     */
    public WatzapResponse sendImageMessage(String phoneNumber, String imageUrl, String caption) {
        logger.debug("Sending image message to {}", phoneNumber);

        var requestBody = new HashMap<String, Object>();
        requestBody.put("api_key", watzapProperties.getApiKey());
        requestBody.put("number_key", watzapProperties.getNumberKey());
        requestBody.put("phone_no", phoneNumber);
        requestBody.put("url", imageUrl);
        requestBody.put("message", caption != null ? caption : "");
        requestBody.put("separate_caption", "0");
        requestBody.put("wait_until_send", "1");

        return sendRequest("/send_image_url", requestBody);
    }

    /**
     * Generic request sender with timeout and retry logic
     */
    private WatzapResponse sendRequest(String endpoint, Map<String, Object> requestBody) {
        try {
            logger.debug("Sending request to Watzap API: endpoint={}, body={}", endpoint, requestBody);
            return webClient.post()
                    .uri(endpoint)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(WatzapResponse.class)
                    .timeout(Duration.ofMillis(watzapProperties.getTimeout().getReadMs()))
                    .retryWhen(Retry.backoff(2, Duration.ofMillis(500)))
                    .block();
        } catch (WebClientResponseException e) {
            throw new NotificationException(
                    "Watzap API error: " + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new NotificationException("Watzap API call failed: " + e.getMessage(), e);
        }
    }
}
