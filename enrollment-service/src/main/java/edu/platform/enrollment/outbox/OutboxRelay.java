package edu.platform.enrollment.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.platform.enrollment.messaging.EnrollmentEvent;
import edu.platform.enrollment.messaging.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.Map;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private static final Duration LEASE_DURATION = Duration.ofSeconds(30);
    private static final int BATCH_SIZE = 10;
    private static final String INSTANCE_ID = UUID.randomUUID().toString().substring(0, 8);

    private final OutboxEventRepository outboxRepository;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    private static final Map<String, String> TYPE_IDS = Map.of(
            "cmd.reserve-seat",      "edu.platform.catalog.messaging.ReserveSeatCommand",
            "cmd.release-seat",      "edu.platform.catalog.messaging.ReleaseSeatCommand",
            "cmd.debit-payment",     "edu.platform.payment.messaging.DebitCommand",
            "cmd.refund-payment",    "edu.platform.payment.messaging.RefundCommand",
            "EnrollmentConfirmed",   "edu.platform.education.notifications.messaging.EnrollmentConfirmedEvent",
            "enrollment.created",    "edu.platform.enrollment.messaging.EnrollmentEvent"
    );

    @Value("${outbox.relay.enabled:true}")
    private boolean enabled;

    @Scheduled(fixedDelayString = "${outbox.relay.interval-ms:2000}")
    public void relay() {
        if (!enabled) return;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime leaseUntil = now.plus(LEASE_DURATION);

        outboxRepository.findFreeEvents(now, BATCH_SIZE)
                .flatMap(event -> processOne(event, now, leaseUntil))
                .subscribe(
                        null,
                        ex -> log.error("[RELAY {}] Ошибка в цикле: {}", INSTANCE_ID, ex.getMessage(), ex)
                );
    }

    private Mono<Void> processOne(OutboxEvent event, LocalDateTime now, LocalDateTime leaseUntil) {
        return outboxRepository.tryLock(event.getId(), now, leaseUntil)
                .flatMap(locked -> {
                    if (locked == 0) {
                        log.debug("[RELAY {}] Событие id={} уже залочено другим relay'ем",
                                INSTANCE_ID, event.getId());
                        return Mono.empty();
                    }

                    log.info("[RELAY {}] Захвачено событие id={}, eventId={}, aggregateId={}",
                            INSTANCE_ID, event.getId(), event.getEventId(), event.getAggregateId());

                    return publish(event);
                });
    }

    private Mono<Void> publish(OutboxEvent event) {
        String routingKey = event.getEventType();
        String typeId = TYPE_IDS.getOrDefault(routingKey, null);

        return eventPublisher.publishRaw(routingKey, event.getEventId(),
                        event.getPayload(), typeId)
                .doOnSuccess(v -> log.info("[RELAY {}] Опубликовано eventId={}, routingKey={}",
                        INSTANCE_ID, event.getEventId(), routingKey))
                .then(outboxRepository.markPublished(event.getId(), LocalDateTime.now()))
                .doOnSuccess(r -> log.info("[RELAY {}] Отмечено published_at eventId={}",
                        INSTANCE_ID, event.getEventId()))
                .onErrorResume(ex -> {
                    log.error("[RELAY {}] Ошибка публикации eventId={}: {}",
                            INSTANCE_ID, event.getEventId(), ex.getMessage());
                    return outboxRepository.markFailed(event.getId(), ex.getMessage());
                })
                .then();
    }
}