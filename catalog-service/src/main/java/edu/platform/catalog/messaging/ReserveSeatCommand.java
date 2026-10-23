package edu.platform.catalog.messaging;

public record ReserveSeatCommand(
        String commandId,
        Long enrollmentId,
        Long courseId
) {}