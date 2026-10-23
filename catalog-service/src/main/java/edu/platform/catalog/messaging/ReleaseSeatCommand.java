package edu.platform.catalog.messaging;

public record ReleaseSeatCommand(
        String commandId,
        Long enrollmentId,
        Long courseId
) {}