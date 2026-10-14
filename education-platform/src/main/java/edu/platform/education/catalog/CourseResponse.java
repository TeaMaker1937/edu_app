package edu.platform.education.catalog;

import java.math.BigDecimal;

public record CourseResponse(
        Long id,
        String title,
        String summary,
        BigDecimal price,
        Long teacherId
) {}