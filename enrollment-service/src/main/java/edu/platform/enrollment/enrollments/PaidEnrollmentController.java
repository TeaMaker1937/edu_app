package edu.platform.enrollment.enrollments;

import edu.platform.enrollment.CatalogClient;
import edu.platform.enrollment.CourseOfferResponse;
import edu.platform.enrollment.saga.SagaCoordinator;
import edu.platform.enrollment.saga.SagaInstance;
import edu.platform.enrollment.saga.SagaInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;

import org.springframework.web.server.ServerWebExchange;
import java.util.List;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/api/enrollments/paid")
@RequiredArgsConstructor
public class PaidEnrollmentController {

    private final EnrollmentRepository enrollmentRepo;
    private final IdempotencyKeyRepository idempotencyRepo;
    private final SagaInstanceRepository sagaRepo;
    private final SagaCoordinator coordinator;
    private final CatalogClient catalogClient;
    private final TransactionalOperator tx;

    @PostMapping
    public Mono<ResponseEntity<Object>> createPaid(
            ServerWebExchange exchange,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody PaidEnrollmentRequest req) {

        Long jwtUserId = exchange.getAttribute("jwt.userId");
        @SuppressWarnings("unchecked")
        List<String> roles = exchange.getAttribute("jwt.roles");

        boolean isStudent = roles != null && roles.contains("ROLE_STUDENT");
        boolean isTeacherOrAdmin = roles != null
                && (roles.contains("ROLE_TEACHER") || roles.contains("ROLE_ADMIN"));

        if (isStudent) {
            if (!Objects.equals(req.userId(), jwtUserId)) {
                log.warn("[PAID] STUDENT {} попытался создать запись для userId={}",
                        jwtUserId, req.userId());
                return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .<Object>body(Map.of(
                                "code", "FORBIDDEN",
                                "message", "STUDENT может создать запись только для себя"
                        )));
            }
        } else if (!isTeacherOrAdmin) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .<Object>body(Map.of(
                            "code", "FORBIDDEN",
                            "message", "Недостаточно прав"
                    )));
        }

        return catalogClient.fetchOffer(req.courseId(), "1.0")
                .flatMap(offer -> startPaidFlow(idempotencyKey, req, offer))
                .<ResponseEntity<Object>>map(saga ->
                        ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                                "sagaId", saga.getId(),
                                "enrollmentId", saga.getEnrollmentId(),
                                "state", saga.getState()
                        )))
                .onErrorResume(ex -> {
                    log.error("[PAID] ошибка создания: {}", ex.getMessage(), ex);
                    return Mono.just(ResponseEntity.status(503)
                            .<Object>body(Map.of(
                                    "code", "PAID_ENROLLMENT_ERROR",
                                    "message", ex.getMessage() == null ? "unknown" : ex.getMessage()
                            )));
                });
    }

    private Mono<SagaInstance> startPaidFlow(String idempotencyKey,
                                             PaidEnrollmentRequest req,
                                             CourseOfferResponse offer) {
        LocalDateTime now = LocalDateTime.now();

        // 1) idempotency_key — обязательна из-за FK (лаба 4)
        IdempotencyKey idKey = IdempotencyKey.builder()
                .idempotencyKey(idempotencyKey)
                .requestHash(sha256(req.courseId() + ":" + req.userId()))
                .responseStatus(0)
                .responseBody("")
                .createdAt(now)
                .build();

        // 2) enrollment (status = PENDING)
        Enrollment enrollment = Enrollment.builder()
                .idempotencyKey(idempotencyKey)
                .userId(req.userId())
                .courseId(offer.courseId())
                .courseTitle(offer.title())
                .price(offer.price())
                .status("PENDING")
                .createdAt(now)
                .build();

        // 3) вся цепочка: idKey → enrollment → saga.start → saga.moveToReserving → reload saga
        //    ОДНА транзакция. FK deferred → проверка при COMMIT.
        Mono<SagaInstance> chain =
                idempotencyRepo.save(idKey)
                        .then(enrollmentRepo.save(enrollment))
                        .flatMap(saved ->
                                coordinator.start(saved.getId(), saved.getCourseId(), saved.getUserId())
                                        .flatMap(saga ->
                                                coordinator.moveToReserving(saga.getId())
                                                        .then(sagaRepo.findById(saga.getId()))));

        return chain.as(tx::transactional);
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 недоступен", e);
        }
    }

    public record PaidEnrollmentRequest(Long courseId, Long userId) {}
}