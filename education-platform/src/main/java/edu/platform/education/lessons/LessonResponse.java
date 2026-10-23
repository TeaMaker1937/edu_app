package edu.platform.education.lessons;

public record LessonResponse(
        Long id,
        Long courseId,
        String title,
        String content,
        Long version
) {}