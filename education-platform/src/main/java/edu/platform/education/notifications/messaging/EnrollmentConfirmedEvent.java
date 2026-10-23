package edu.platform.education.notifications.messaging;

import java.time.LocalDateTime;

public record EnrollmentConfirmedEvent(
        String eventId,
        Long enrollmentId,
        Long courseId,
        Long userId,
        String courseTitle,
        LocalDateTime occurredAt
) {}