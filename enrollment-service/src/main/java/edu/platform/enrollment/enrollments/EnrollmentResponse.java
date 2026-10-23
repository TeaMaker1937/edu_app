package edu.platform.enrollment.enrollments;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EnrollmentResponse(
        Long id,
        Long courseId,
        Long userId,
        String courseTitle,
        BigDecimal price,
        String status,
        LocalDateTime createdAt,
        String eventId
) {}