package edu.platform.payment.messaging;

import java.math.BigDecimal;

public record DebitCommand(
        String commandId,
        Long enrollmentId,
        Long userId,
        BigDecimal amount
) {}