package com.vibe.notification.infrastructure.adapter.email;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Email configuration properties
 */
@Component
@ConfigurationProperties(prefix = "spring.mail")
public class EmailProperties {
    private String username;
    private String password;
    private String host;
    private int port;
    private String fromAddress;

    // Constructors & Getters/Setters
    public EmailProperties() {}

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getFromAddress() {
        return fromAddress != null ? fromAddress : username;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }
}
