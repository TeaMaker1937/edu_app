package edu.platform.enrollment;

import java.math.BigDecimal;

public record CourseOfferResponse(
        Long courseId,
        String title,
        BigDecimal price
) {}