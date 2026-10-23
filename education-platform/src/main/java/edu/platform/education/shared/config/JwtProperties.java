package edu.platform.education.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long expiration,
        String issuer,
        String audience
) {
    public JwtProperties {
        if (issuer == null || issuer.isBlank()) issuer = "edu-platform";
        if (audience == null || audience.isBlank()) audience = "education-platform";
    }
}