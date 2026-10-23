package edu.platform.catalog.domain;

import edu.platform.catalog.messaging.ReleaseSeatCommand;
import edu.platform.catalog.messaging.ReserveSeatCommand;
import edu.platform.catalog.messaging.SeatReleasedReply;
import edu.platform.catalog.messaging.SeatReservedReply;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatReservationService {

    private final SeatOperationRepository ops;
    private final DatabaseClient db;
    private final TransactionalOperator tx;

    // ---------- RESERVE ----------

    public Mono<SeatReservedReply> reserve(ReserveSeatCommand cmd) {
        return ops.findByCommandIdAndAction(cmd.commandId(), "RESERVE")
                .map(existing -> {
                    log.info("[CATALOG] idempotent reserve: commandId={}, result={}",
                            cmd.commandId(), existing.getResult());
                    return reply(cmd, existing);
                })
                .switchIfEmpty(tx.transactional(doReserve(cmd)));
    }

    private Mono<SeatReservedReply> doReserve(ReserveSeatCommand cmd) {
        // 1) атомарно инкрементим, только если курс существует
        return db.sql("""
                UPDATE public.courses
                   SET reserved_seats = reserved_seats + 1
                 WHERE id = :id
                RETURNING reserved_seats
                """)
                .bind("id", cmd.courseId())
                .map(row -> row.get("reserved_seats", Integer.class))
                .one()
                .flatMap(newValue -> {
                    log.info("[CATALOG] RESERVED: courseId={}, reserved_seats={}",
                            cmd.courseId(), newValue);
                    return ops.save(SeatOperation.builder()
                                    .commandId(cmd.commandId()).action("RESERVE")
                                    .enrollmentId(cmd.enrollmentId()).courseId(cmd.courseId())
                                    .result("RESERVED").createdAt(OffsetDateTime.now())
                                    .build())
                            .map(op -> new SeatReservedReply(cmd.commandId(),
                                    cmd.enrollmentId(), cmd.courseId(), true, null));
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // курс не найден
                    return ops.save(SeatOperation.builder()
                                    .commandId(cmd.commandId()).action("RESERVE")
                                    .enrollmentId(cmd.enrollmentId()).courseId(cmd.courseId())
                                    .result("NOT_FOUND").reason("course not found")
                                    .createdAt(OffsetDateTime.now())
                                    .build())
                            .map(op -> new SeatReservedReply(cmd.commandId(),
                                    cmd.enrollmentId(), cmd.courseId(), false, "course not found"));
                }));
    }

    // ---------- RELEASE ----------

    public Mono<SeatReleasedReply> release(ReleaseSeatCommand cmd) {
        return ops.findByCommandIdAndAction(cmd.commandId(), "RELEASE")
                .map(existing -> {
                    log.info("[CATALOG] idempotent release: commandId={}, result={}",
                            cmd.commandId(), existing.getResult());
                    return new SeatReleasedReply(cmd.commandId(), cmd.enrollmentId(),
                            cmd.courseId(), "RELEASED".equals(existing.getResult()),
                            existing.getReason());
                })
                .switchIfEmpty(tx.transactional(doRelease(cmd)));
    }

    private Mono<SeatReleasedReply> doRelease(ReleaseSeatCommand cmd) {
        // освобождаем, не уходя в отрицательные значения
        return db.sql("""
                UPDATE public.courses
                   SET reserved_seats = GREATEST(reserved_seats - 1, 0)
                 WHERE id = :id
                RETURNING reserved_seats
                """)
                .bind("id", cmd.courseId())
                .map(row -> row.get("reserved_seats", Integer.class))
                .one()
                .flatMap(newValue -> {
                    log.info("[CATALOG] RELEASED: courseId={}, reserved_seats={}",
                            cmd.courseId(), newValue);
                    return ops.save(SeatOperation.builder()
                                    .commandId(cmd.commandId()).action("RELEASE")
                                    .enrollmentId(cmd.enrollmentId()).courseId(cmd.courseId())
                                    .result("RELEASED").createdAt(OffsetDateTime.now())
                                    .build())
                            .map(op -> new SeatReleasedReply(cmd.commandId(),
                                    cmd.enrollmentId(), cmd.courseId(), true, null));
                })
                .switchIfEmpty(Mono.defer(() -> ops.save(SeatOperation.builder()
                                .commandId(cmd.commandId()).action("RELEASE")
                                .enrollmentId(cmd.enrollmentId()).courseId(cmd.courseId())
                                .result("NOT_FOUND").reason("course not found")
                                .createdAt(OffsetDateTime.now())
                                .build())
                        .map(op -> new SeatReleasedReply(cmd.commandId(),
                                cmd.enrollmentId(), cmd.courseId(), false, "course not found"))));
    }

    private SeatReservedReply reply(ReserveSeatCommand cmd, SeatOperation op) {
        return new SeatReservedReply(cmd.commandId(), cmd.enrollmentId(), cmd.courseId(),
                "RESERVED".equals(op.getResult()), op.getReason());
    }
}