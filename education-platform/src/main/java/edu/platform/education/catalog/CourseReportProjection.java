package edu.platform.education.catalog;

/**
 * Проекция для отчёта по курсам.
 * Spring Data JPA сопоставит колонки из native-запроса
 * с геттерами через алиасы: courseId, title, activeCount.
 */
public interface CourseReportProjection {
    Long getCourseId();
    String getTitle();
    Long getActiveCount();
}