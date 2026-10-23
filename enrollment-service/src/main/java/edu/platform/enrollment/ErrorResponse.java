package edu.platform.enrollment;

public record ErrorResponse(
        String code,
        String message
) {}