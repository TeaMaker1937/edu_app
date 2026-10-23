package edu.platform.enrollment.saga;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import org.springframework.data.r2dbc.repository.Modifying;

public interface SagaInstanceRepository extends ReactiveCrudRepository<SagaInstance, Long> {

    Mono<SagaInstance> findByEnrollmentId(Long enrollmentId);

    @Query("""
            SELECT * FROM enrollment.saga_instances
            WHERE state IN ('RESERVING','DEBITING','COMPENSATING')
            """)
    Flux<SagaInstance> findActive();

    @Query("""
            SELECT * FROM enrollment.saga_instances
            WHERE state IN ('RESERVING','DEBITING')
              AND deadline_at < :now
            """)
    Flux<SagaInstance> findExpired(LocalDateTime now);

    @Modifying
    @Query("""
        UPDATE enrollment.saga_instances
           SET state = 'COMPENSATING',
               current_command_id = :cmdId,
               deadline_at = NULL,
               retry_count = 0,
               release_done = FALSE,
               refund_done = FALSE,
               updated_at = :now
         WHERE id = :id
           AND state IN ('NEW','RESERVING','DEBITING')
        """)
    Mono<Integer> tryStartCompensation(Long id, String cmdId, LocalDateTime now);

    @Modifying
    @Query("""
        UPDATE enrollment.saga_instances
           SET release_done = TRUE, updated_at = :now
         WHERE id = :id
        """)
    Mono<Integer> markReleaseDone(Long id, LocalDateTime now);

    @Modifying
    @Query("""
        UPDATE enrollment.saga_instances
           SET refund_done = TRUE, updated_at = :now
         WHERE id = :id
        """)
    Mono<Integer> markRefundDone(Long id, LocalDateTime now);
}