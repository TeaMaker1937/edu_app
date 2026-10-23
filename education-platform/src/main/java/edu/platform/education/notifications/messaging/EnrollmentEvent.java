package edu.platform.education.notifications.messaging;

import java.time.LocalDateTime;

public record EnrollmentEvent(
        String eventId,
        Long enrollmentId,
        Long courseId,
        Long userId,
        LocalDateTime occurredAt
) {}