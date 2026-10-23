package edu.platform.payment.messaging;

public record DebitedReply(
        String commandId,
        Long enrollmentId,
        boolean success,
        String reason
) {}