package edu.platform.payment.messaging;

public record RefundedReply(
        String commandId,
        Long enrollmentId,
        boolean success,
        String reason
) {}