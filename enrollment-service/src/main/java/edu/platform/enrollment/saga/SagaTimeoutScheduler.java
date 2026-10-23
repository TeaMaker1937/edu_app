package edu.platform.enrollment.saga;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class SagaTimeoutScheduler {

    private final SagaInstanceRepository sagaRepo;
    private final SagaCoordinator coordinator;

    @Scheduled(fixedDelay = 5000)
    public void checkTimeouts() {
        sagaRepo.findExpired(LocalDateTime.now())
                .flatMap(saga -> {
                    log.warn("[SAGA] Timeout saga {} в состоянии {}, переходим в COMPENSATING",
                            saga.getId(), saga.getState());
                    return coordinator.startCompensation(saga.getId(), "timeout in " + saga.getState());
                })
                .subscribe();
    }
}