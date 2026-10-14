package edu.platform.education.lessons;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LessonUpdateRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank String content,
        @NotNull Long version
) {}