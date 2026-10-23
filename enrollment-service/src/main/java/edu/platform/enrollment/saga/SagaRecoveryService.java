package edu.platform.enrollment.saga;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class SagaRecoveryService {

    private final SagaInstanceRepository sagaRepo;
    private final SagaCoordinator coordinator;

    /**
     * После старта координатора читаем активные саги и пытаемся их продвинуть.
     * Логика простая: если в RESERVING/DEBITING и есть currentCommandId — переопубликовать команду
     * (idempotent — если она уже была обработана, получим тот же reply).
     * В COMPENSATING — переопубликовать release и refund.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void recover() {
        sagaRepo.findActive()
                .flatMap(saga -> {
                    if (SagaState.COMPENSATING.name().equals(saga.getState())) {
                        log.warn("[SAGA][RECOVERY] saga {} в COMPENSATING — retry компенсации", saga.getId());
                        return coordinator.startCompensation(saga.getId(),
                                "recovery: retry compensation");
                    }
                    log.warn("[SAGA][RECOVERY] saga {} в {} — ждём timeout scheduler",
                            saga.getId(), saga.getState());
                    return Mono.empty();
                })
                .subscribe();
    }
}