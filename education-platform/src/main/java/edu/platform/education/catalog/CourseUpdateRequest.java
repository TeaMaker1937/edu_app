package edu.platform.education.catalog;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CourseUpdateRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank String summary,
        @NotNull @DecimalMin(value = "0.0", message = "Цена не может быть отрицательной")
        BigDecimal price,
        Long version   // для optimistic lock, опциональный
) {}