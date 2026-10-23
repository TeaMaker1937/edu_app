package edu.platform.enrollment.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.platform.enrollment.enrollments.Enrollment;
import edu.platform.enrollment.enrollments.EnrollmentRepository;
import edu.platform.enrollment.messaging.RabbitConfig;
import edu.platform.enrollment.saga.messaging.SagaMessages.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class SagaReplyListener {

    private final SagaCoordinator coordinator;
    private final SagaInstanceRepository sagaRepo;
    private final EnrollmentRepository enrollmentRepo;   // ← для price
    private final ObjectMapper objectMapper;
    private final TransactionalOperator tx;
    private static final int MAX_COMPENSATION_ATTEMPTS = 3;

    @RabbitListener(queues = RabbitConfig.REPLY_QUEUE,
            containerFactory = "replyListenerContainerFactory")   // ← ЭТО
    public void onReply(Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        String json = new String(message.getBody(), StandardCharsets.UTF_8);
        log.info("[SAGA][REPLY] routingKey={}, body={}", routingKey, json);

        try {
            switch (routingKey) {
                case "reply.seat-reserved" -> handleSeatReserved(
                        objectMapper.readValue(json, SeatReservedReply.class));
                case "reply.seat-released" -> handleSeatReleased(
                        objectMapper.readValue(json, SeatReleasedReply.class));
                case "reply.payment-debited" -> handlePaymentDebited(
                        objectMapper.readValue(json, PaymentDebitedReply.class));
                case "reply.payment-refunded" -> handlePaymentRefunded(
                        objectMapper.readValue(json, PaymentRefundedReply.class));
                default -> log.warn("[SAGA][REPLY] неизвестный routingKey={}", routingKey);
            }
        } catch (Exception e) {
            log.error("[SAGA][REPLY] ошибка обработки: {}", e.getMessage(), e);
        }
    }

    private void handleSeatReserved(SeatReservedReply r) {
        sagaRepo.findByEnrollmentId(r.enrollmentId())
                .flatMap(saga -> {
                    if (!SagaState.RESERVING.name().equals(saga.getState())) {
                        log.warn("[SAGA] seat-reserved пришёл, но saga {} в {}",
                                saga.getId(), saga.getState());
                        return Mono.empty();
                    }
                    if (!r.success()) {
                        return coordinator.startCompensation(saga.getId(),
                                "reserve failed: " + r.reason()).as(tx::transactional);
                    }
                    // цена — из enrollment, чтобы платёж был на реальную сумму
                    return enrollmentRepo.findById(saga.getEnrollmentId())
                            .map(Enrollment::getPrice)
                            .defaultIfEmpty(BigDecimal.ZERO)
                            .flatMap(price -> coordinator.moveToDebiting(saga.getId(), price)
                                    .as(tx::transactional));
                })
                .subscribe();
    }

    private void handlePaymentDebited(PaymentDebitedReply r) {
        sagaRepo.findByEnrollmentId(r.enrollmentId())
                .flatMap(saga -> {
                    // Late debit — компенсация уже началась или завершена
                    if (SagaState.COMPENSATING.name().equals(saga.getState())
                            || SagaState.CANCELLED.name().equals(saga.getState())) {
                        if (r.success()) {
                            log.warn("[SAGA] Поздний debit при {} — доп. refund",
                                    saga.getState());
                            return coordinator.sendExtraRefund(saga.getId(),
                                    "late debit after " + saga.getState());
                        }
                        return Mono.empty();
                    }
                    if (!SagaState.DEBITING.name().equals(saga.getState())) return Mono.empty();

                    Mono<Void> action = r.success()
                            ? coordinator.moveToConfirmed(saga.getId())
                            : coordinator.startCompensation(saga.getId(),
                            "payment rejected: " + r.reason());
                    return action.as(tx::transactional);
                })
                .subscribe();
    }

    private void handleSeatReleased(SeatReleasedReply r) {
        sagaRepo.findByEnrollmentId(r.enrollmentId())
                .filter(s -> SagaState.COMPENSATING.name().equals(s.getState()))
                .flatMap(s -> {
                    if (!r.success()) {
                        return onComponentFailure(s, "release", r.reason());
                    }
                    return sagaRepo.markReleaseDone(s.getId(), LocalDateTime.now())
                            .flatMap(rc -> coordinator.tryCompleteCompensation(s.getId()))
                            .as(tx::transactional);
                })
                .subscribe();
    }

    private void handlePaymentRefunded(PaymentRefundedReply r) {
        sagaRepo.findByEnrollmentId(r.enrollmentId())
                .filter(s -> SagaState.COMPENSATING.name().equals(s.getState()))
                .flatMap(s -> {
                    if (!r.success()) {
                        return onComponentFailure(s, "refund", r.reason());
                    }
                    return sagaRepo.markRefundDone(s.getId(), LocalDateTime.now())
                            .flatMap(rc -> coordinator.tryCompleteCompensation(s.getId()))
                            .as(tx::transactional);
                })
                .subscribe();
    }

    private Mono<Void> onComponentFailure(SagaInstance saga, String component, String reason) {
        return coordinator.incrementRetry(saga.getId()).flatMap(attempts -> {
            if (attempts >= MAX_COMPENSATION_ATTEMPTS) {
                log.error("[SAGA] saga {} — {} провалился {} раз: {} → MANUAL_REVIEW",
                        saga.getId(), component, attempts, reason);
                return coordinator.moveToManualReview(saga.getId(),
                                component + " failed " + attempts + " times: " + reason)
                        .as(tx::transactional);
            }
            log.warn("[SAGA] saga {} — {} попытка {} не удалась: {} — retry",
                    saga.getId(), component, attempts, reason);
            return coordinator.retryComponent(saga.getId(), component, reason)
                    .as(tx::transactional);
        });
    }
}