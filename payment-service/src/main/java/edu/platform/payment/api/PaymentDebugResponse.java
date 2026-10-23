package edu.platform.payment.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentDebugResponse(
        Long id,
        String commandId,
        Long enrollmentId,
        BigDecimal amount,
        String status,
        OffsetDateTime createdAt
) {}