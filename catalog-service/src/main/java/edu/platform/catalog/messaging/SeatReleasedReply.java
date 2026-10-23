package edu.platform.catalog.messaging;

public record SeatReleasedReply(
        String commandId,
        Long enrollmentId,
        Long courseId,
        boolean success,
        String reason
) {}