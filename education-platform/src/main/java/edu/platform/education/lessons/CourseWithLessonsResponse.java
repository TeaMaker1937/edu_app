package edu.platform.education.lessons;

import java.math.BigDecimal;
import java.util.List;

public record CourseWithLessonsResponse(
        Long id,
        String title,
        String summary,
        BigDecimal price,
        Long teacherId,
        List<LessonSummary> lessons
) {
    public record LessonSummary(Long id, String title, Long version) {}
}