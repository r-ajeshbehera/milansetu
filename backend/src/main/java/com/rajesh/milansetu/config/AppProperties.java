package com.rajesh.milansetu.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, RateLimit rateLimit, Otp otp, Notifications notifications, Jobs jobs) {
    public record Jwt(String secret, long expirationMinutes) { }
    public record Cors(List<String> allowedOrigins) { }
    public record RateLimit(int publicPerMinute, int loginPerMinute) { }
    /** expiryMinutes = 10, maxAttempts = 5, maxIssuesPerHour = 3 by default. */
    public record Otp(int expiryMinutes, int maxAttempts, int maxIssuesPerHour, int retentionDays) { }
    /** mode = mock | email. */
    public record Notifications(String mode, String fromAddress) { }
    public record Jobs(int staleAfterMinutes) { }
}
