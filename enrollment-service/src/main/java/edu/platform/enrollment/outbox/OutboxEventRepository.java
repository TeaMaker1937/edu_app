package edu.platform.enrollment.outbox;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface OutboxEventRepository extends ReactiveCrudRepository<OutboxEvent, Long> {

    /** Выбрать свободные события: не опубликованы и lease не активен. */
    @Query("""
            SELECT * FROM enrollment.outbox_events
            WHERE published_at IS NULL
              AND (locked_until IS NULL OR locked_until < :now)
            ORDER BY created_at
            LIMIT :limit
            """)
    Flux<OutboxEvent> findFreeEvents(LocalDateTime now, int limit);

    /** Атомарно захватить событие. rowcount=1 = захват удался, 0 = уже залочено другим. */
    @Modifying
    @Query("""
            UPDATE enrollment.outbox_events
            SET locked_until = :leaseUntil
            WHERE id = :id
              AND published_at IS NULL
              AND (locked_until IS NULL OR locked_until < :now)
            """)
    Mono<Integer> tryLock(Long id, LocalDateTime now, LocalDateTime leaseUntil);

    /** Пометить опубликованным. */
    @Modifying
    @Query("""
            UPDATE enrollment.outbox_events
            SET published_at = :now, locked_until = NULL, attempts = attempts + 1, last_error = NULL
            WHERE id = :id
            """)
    Mono<Integer> markPublished(Long id, LocalDateTime now);

    /** Зафиксировать ошибку публикации (attempts++, last_error). Освободить lease. */
    @Modifying
    @Query("""
            UPDATE enrollment.outbox_events
            SET attempts = attempts + 1, last_error = :error, locked_until = NULL
            WHERE id = :id
            """)
    Mono<Integer> markFailed(Long id, String error);
}