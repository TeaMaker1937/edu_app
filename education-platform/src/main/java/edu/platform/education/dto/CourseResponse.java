package edu.platform.education.dto;

public record CourseResponse(
        Long id,
        String title,
        String description,
        Long teacherId
) {}