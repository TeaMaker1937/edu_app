package edu.platform.enrollment.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.platform.enrollment.outbox.OutboxEvent;
import edu.platform.enrollment.outbox.OutboxEventRepository;
import edu.platform.enrollment.saga.messaging.SagaMessages.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import org.springframework.transaction.annotation.Transactional;
import edu.platform.enrollment.enrollments.EnrollmentRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SagaCoordinator {

    private static final Duration STEP_TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_COMPENSATION_ATTEMPTS = 3;

    private final SagaInstanceRepository sagaRepo;
    private final SagaTransitionRepository transitionRepo;
    private final OutboxEventRepository outboxRepo;
    private final ObjectMapper objectMapper;
    private final EnrollmentRepository enrollmentRepo;

    // ---------- Старт саги ----------

    
    public Mono<SagaInstance> start(Long enrollmentId, Long courseId, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        SagaInstance saga = SagaInstance.builder()
                .enrollmentId(enrollmentId).courseId(courseId).userId(userId)
                .state(SagaState.NEW.name())
                .retryCount(0).createdAt(now).updatedAt(now)
                .build();
        return sagaRepo.save(saga);
    }

    // ---------- Переход NEW → RESERVING + команда reserve-seat ----------

    
    public Mono<Void> moveToReserving(Long sagaId) {
        return sagaRepo.findById(sagaId)
                .flatMap(saga -> {
                    String cmdId = UUID.randomUUID().toString();
                    LocalDateTime now = LocalDateTime.now();
                    saga.setState(SagaState.RESERVING.name());
                    saga.setCurrentCommandId(cmdId);
                    saga.setDeadlineAt(now.plus(STEP_TIMEOUT));
                    saga.setUpdatedAt(now);
                    return sagaRepo.save(saga)
                            .then(transitionRepo.save(transition(sagaId, SagaState.NEW.name(), SagaState.RESERVING, "start", cmdId)))
                            .then(outboxRepo.save(command(cmdId,
                                    "cmd.reserve-seat",
                                    new ReserveSeatCommand(cmdId, saga.getEnrollmentId(), saga.getCourseId()))))
                            .then();
                });
    }

    // ---------- RESERVING → DEBITING + команда debit-payment ----------

    
    public Mono<Void> moveToDebiting(Long sagaId, BigDecimal amount) {
        return sagaRepo.findById(sagaId)
                .flatMap(saga -> {
                    if (!SagaState.RESERVING.name().equals(saga.getState())) {
                        log.warn("[SAGA] moveToDebiting: saga {} в состоянии {}, ожидали RESERVING — SKIP",
                                sagaId, saga.getState());
                        return Mono.empty();
                    }
                    String cmdId = UUID.randomUUID().toString();
                    LocalDateTime now = LocalDateTime.now();
                    saga.setState(SagaState.DEBITING.name());
                    saga.setCurrentCommandId(cmdId);
                    saga.setDeadlineAt(now.plus(STEP_TIMEOUT));
                    saga.setUpdatedAt(now);
                    return sagaRepo.save(saga)
                            .then(transitionRepo.save(transition(sagaId, SagaState.RESERVING.name(), SagaState.DEBITING, "seat reserved", cmdId)))
                            .then(outboxRepo.save(command(cmdId,
                                    "cmd.debit-payment",
                                    new DebitPaymentCommand(cmdId, saga.getEnrollmentId(), saga.getUserId(), amount))))
                            .then();
                });
    }

    // ---------- DEBITING → CONFIRMED ----------


    public Mono<Void> moveToConfirmed(Long sagaId) {
        return sagaRepo.findById(sagaId)
                .flatMap(saga -> {
                    if (!SagaState.DEBITING.name().equals(saga.getState())) {
                        log.warn("[SAGA] moveToConfirmed: saga {} в состоянии {} — SKIP",
                                sagaId, saga.getState());
                        return Mono.empty();
                    }
                    return enrollmentRepo.findById(saga.getEnrollmentId())
                            .flatMap(enrollment -> {
                                LocalDateTime now = LocalDateTime.now();
                                String eventId = UUID.randomUUID().toString();

                                saga.setState(SagaState.CONFIRMED.name());
                                saga.setCurrentCommandId(null);
                                saga.setDeadlineAt(null);
                                saga.setUpdatedAt(now);

                                EnrollmentConfirmedEvent payload = new EnrollmentConfirmedEvent(
                                        eventId,
                                        saga.getEnrollmentId(),
                                        saga.getCourseId(),
                                        saga.getUserId(),
                                        enrollment.getCourseTitle(),
                                        now
                                );

                                return sagaRepo.save(saga)
                                        .then(transitionRepo.save(transition(sagaId,
                                                SagaState.DEBITING.name(), SagaState.CONFIRMED,
                                                "payment ok", null)))
                                        .then(outboxRepo.save(OutboxEvent.builder()
                                                .eventId(eventId)
                                                .eventType("EnrollmentConfirmed")
                                                .aggregateId(saga.getEnrollmentId())
                                                .payload(serialize(payload))
                                                .createdAt(now)
                                                .attempts(0)
                                                .build()))
                                        .then();
                            });
                });
    }

    // ---------- Вход в COMPENSATING ----------


    public Mono<Void> startCompensation(Long sagaId, String reason) {
        return sagaRepo.findById(sagaId).flatMap(saga -> {
            String releaseCmdId = UUID.randomUUID().toString();
            String refundCmdId  = UUID.randomUUID().toString();
            LocalDateTime now = LocalDateTime.now();

            return sagaRepo.tryStartCompensation(sagaId, releaseCmdId, now)
                    .flatMap(rowcount -> {
                        if (rowcount == 0) {
                            log.info("[SAGA] startCompensation: saga {} уже компенсируется — SKIP", sagaId);
                            return Mono.empty();
                        }
                        log.warn("[SAGA] saga {} переходит в COMPENSATING: {}", sagaId, reason);
                        return transitionRepo.save(transition(sagaId,
                                        saga.getState(), SagaState.COMPENSATING, reason, releaseCmdId))
                                .then(outboxRepo.save(command(releaseCmdId, "cmd.release-seat",
                                        new ReleaseSeatCommand(releaseCmdId,
                                                saga.getEnrollmentId(), saga.getCourseId()))))
                                .then(outboxRepo.save(command(refundCmdId, "cmd.refund-payment",
                                        new RefundPaymentCommand(refundCmdId,
                                                saga.getEnrollmentId(), saga.getUserId(), BigDecimal.ZERO))))
                                .then();
                    });
        });
    }

    public Mono<Void> tryCompleteCompensation(Long sagaId) {
        return sagaRepo.findById(sagaId).flatMap(saga -> {
            if (!SagaState.COMPENSATING.name().equals(saga.getState())) return Mono.empty();
            if (!Boolean.TRUE.equals(saga.getReleaseDone())) return Mono.empty();
            if (!Boolean.TRUE.equals(saga.getRefundDone())) return Mono.empty();
            log.info("[SAGA] saga {} — обе компенсации подтверждены → CANCELLED", sagaId);
            return moveToCancelled(sagaId);
        });
    }

    public Mono<Void> retryComponent(Long sagaId, String component, String reason) {
        return sagaRepo.findById(sagaId).flatMap(saga -> {
            String cmdId = UUID.randomUUID().toString();
            OutboxEvent cmd = switch (component) {
                case "release" -> command(cmdId, "cmd.release-seat",
                        new ReleaseSeatCommand(cmdId, saga.getEnrollmentId(), saga.getCourseId()));
                case "refund"  -> command(cmdId, "cmd.refund-payment",
                        new RefundPaymentCommand(cmdId, saga.getEnrollmentId(), saga.getUserId(), BigDecimal.ZERO));
                default -> null;
            };
            if (cmd == null) return Mono.empty();
            log.warn("[SAGA] saga {} — retry {}: {}", sagaId, component, reason);
            return outboxRepo.save(cmd).then();
        });
    }

    // ---------- Компенсация завершена → CANCELLED ----------


    @Transactional
    public Mono<Void> moveToCancelled(Long sagaId) {
        return sagaRepo.findById(sagaId)
                .flatMap(saga -> {
                    if (!SagaState.COMPENSATING.name().equals(saga.getState())) {
                        log.debug("[SAGA] moveToCancelled: saga {} в {}, ожидали COMPENSATING — SKIP",
                                sagaId, saga.getState());
                        return Mono.empty();
                    }
                    LocalDateTime now = LocalDateTime.now();
                    saga.setState(SagaState.CANCELLED.name());
                    saga.setCurrentCommandId(null);
                    saga.setUpdatedAt(now);
                    return sagaRepo.save(saga)
                            .then(transitionRepo.save(transition(sagaId,
                                    SagaState.COMPENSATING.name(), SagaState.CANCELLED,
                                    "all compensations done", null)))
                            .then();
                });
    }

    // ---------- Компенсация провалилась → MANUAL_REVIEW ----------

    
    public Mono<Void> moveToManualReview(Long sagaId, String reason) {
        return sagaRepo.findById(sagaId)
                .flatMap(saga -> {
                    String from = saga.getState();
                    LocalDateTime now = LocalDateTime.now();
                    saga.setState(SagaState.MANUAL_REVIEW.name());
                    saga.setUpdatedAt(now);
                    return sagaRepo.save(saga)
                            .then(transitionRepo.save(transition(sagaId, from,
                                    SagaState.MANUAL_REVIEW, reason, null)))
                            .then();
                });
    }

    // ---------- Инкремент retry ----------

    
    public Mono<Integer> incrementRetry(Long sagaId) {
        return sagaRepo.findById(sagaId).flatMap(saga -> {
            int next = (saga.getRetryCount() == null ? 0 : saga.getRetryCount()) + 1;
            saga.setRetryCount(next);
            saga.setUpdatedAt(LocalDateTime.now());
            return sagaRepo.save(saga).thenReturn(next);
        });
    }

    // ---------- Утилиты ----------

    private SagaTransition transition(Long sagaId, String from, SagaState to, String reason, String cmdId) {
        return SagaTransition.builder()
                .sagaId(sagaId)
                .fromState(from)              // ← уже String, .name() не нужен
                .toState(to.name())
                .reason(reason).commandId(cmdId).createdAt(LocalDateTime.now()).build();
    }

    private OutboxEvent command(String cmdId, String routingKey, Object payload) {
        return OutboxEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(routingKey)
                .aggregateId(0L)
                .payload(serialize(payload))
                .createdAt(LocalDateTime.now())
                .attempts(0)
                .build();
    }

    private String serialize(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { throw new IllegalStateException("serialize fail", e); }
    }

    /**
     * Отправляет дополнительный refund (для late debit после компенсации).
     * State НЕ меняет: либо остаёмся в COMPENSATING (и ждём reply),
     * либо в CANCELLED (деньги просто вернутся, state уже не изменится).
     */
    public Mono<Void> sendExtraRefund(Long sagaId, String reason) {
        return sagaRepo.findById(sagaId)
                .flatMap(saga -> {
                    String refundCmdId = UUID.randomUUID().toString();
                    log.warn("[SAGA] Отправка доп. refund для saga {}: {}", sagaId, reason);
                    return outboxRepo.save(command(refundCmdId,
                                    "cmd.refund-payment",
                                    new RefundPaymentCommand(refundCmdId, saga.getEnrollmentId(),
                                            saga.getUserId(), BigDecimal.ZERO)))
                            .then();
                });
    }

    // ---------- DTO для события EnrollmentConfirmed ----------

    public record EnrollmentConfirmedEvent(
            String eventId,
            Long enrollmentId,
            Long courseId,
            Long userId,
            String courseTitle,
            LocalDateTime occurredAt
    ) {}
}