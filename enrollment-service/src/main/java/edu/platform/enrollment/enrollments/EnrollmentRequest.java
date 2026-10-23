package edu.platform.enrollment.enrollments;

import jakarta.validation.constraints.NotNull;

public record EnrollmentRequest(
        @NotNull Long courseId,
        @NotNull Long userId
) {}