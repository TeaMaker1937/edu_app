package edu.platform.catalog;

public record NotificationTemplateResponse(
        Long courseId,
        String title,
        String template
) {}