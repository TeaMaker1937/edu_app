package edu.platform.education.media;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "media")
public record MediaProperties(
        String storageDir,
        long maxSizeBytes
) {}