package edu.platform.catalog.messaging;

public record SeatReservedReply(
        String commandId,
        Long enrollmentId,
        Long courseId,
        boolean success,
        String reason
) {}