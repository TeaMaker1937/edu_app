package edu.platform.education.media;

import java.time.LocalDateTime;

public record MediaResponse(
        Long id,
        String originalName,
        String contentType,
        long sizeBytes,
        Long uploadedBy,
        LocalDateTime createdAt
) {}