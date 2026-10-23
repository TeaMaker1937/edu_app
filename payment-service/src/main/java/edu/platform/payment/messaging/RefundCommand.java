package edu.platform.payment.messaging;

public record RefundCommand(
        String commandId,      // ВАЖНО: это *тот же* commandId, что у debit (или парный refund-commandId)
        Long enrollmentId,
        Long userId,
        java.math.BigDecimal amount
) {}