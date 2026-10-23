package edu.platform.education.catalog;

public record CourseReportResponse(
        Long courseId,
        String title,
        Long activeCount,
        Long notificationCount
) {}