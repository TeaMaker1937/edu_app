package edu.platform.enrollment.enrollments;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EnrollmentV2Response(
        // поля V1 — те же имена и смысл
        Long id,
        Long courseId,
        Long userId,
        String courseTitle,
        BigDecimal price,
        String status,
        LocalDateTime createdAt,
        // новые поля V2 (вариант 4: notification)
        CourseOfferView offer,
        String notificationTemplate,
        boolean notificationAvailable
) {
    public record CourseOfferView(
            Long courseId,
            String title,
            BigDecimal price
    ) {}
}