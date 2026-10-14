package edu.platform.enrollment;

public record NotificationTemplateResponse(
        Long courseId,
        String title,
        String template
) {}