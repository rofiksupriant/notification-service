package com.vibe.notification.infrastructure.external.watzap;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Watzap configuration properties
 */
@Component
@ConfigurationProperties(prefix = "watzap")
public class WatzapProperties {
    private String apiKey;
    private String numberKey;
    private String baseUrl;
    private Timeout timeout = new Timeout();

    public static class Timeout {
        private int connectMs = 5000;
        private int readMs = 10000;

        public int getConnectMs() {
            return connectMs;
        }

        public void setConnectMs(int connectMs) {
            this.connectMs = connectMs;
        }

        public int getReadMs() {
            return readMs;
        }

        public void setReadMs(int readMs) {
            this.readMs = readMs;
        }
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getNumberKey() {
        return numberKey;
    }

    public void setNumberKey(String numberKey) {
        this.numberKey = numberKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Timeout getTimeout() {
        return timeout;
    }

    public void setTimeout(Timeout timeout) {
        this.timeout = timeout;
    }
}
