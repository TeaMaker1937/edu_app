package edu.platform.enrollment;

import lombok.Getter;

@Getter
public class CourseNotFoundInCatalogException extends RuntimeException {
    private final Long courseId;

    public CourseNotFoundInCatalogException(Long courseId) {
        super("Курс с id " + courseId + " не найден в catalog");
        this.courseId = courseId;
    }

}