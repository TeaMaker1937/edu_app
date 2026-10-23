package edu.platform.enrollment.saga.messaging;

import java.math.BigDecimal;

public final class SagaMessages {
    private SagaMessages() {}

    public record ReserveSeatCommand(String commandId, Long enrollmentId, Long courseId) {}
    public record ReleaseSeatCommand(String commandId, Long enrollmentId, Long courseId) {}

    public record DebitPaymentCommand(
            String commandId, Long enrollmentId, Long userId, BigDecimal amount) {}
    public record RefundPaymentCommand(
            String commandId, Long enrollmentId, Long userId, BigDecimal amount) {}

    public record SeatReservedReply(
            String commandId, Long enrollmentId, Long courseId, boolean success, String reason) {}
    public record SeatReleasedReply(
            String commandId, Long enrollmentId, Long courseId, boolean success, String reason) {}
    public record PaymentDebitedReply(
            String commandId, Long enrollmentId, boolean success, String reason) {}
    public record PaymentRefundedReply(
            String commandId, Long enrollmentId, boolean success, String reason) {}
}