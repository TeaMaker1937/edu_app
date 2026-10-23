package edu.platform.enrollment.enrollments;

import edu.platform.enrollment.messaging.EnrollmentEvent;
import edu.platform.enrollment.messaging.EventPublisher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
@Tag(name = "Записи на курсы", description = "Idempotent enrollment (Lab 4, п.4)")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final EventPublisher eventPublisher;

    @Operation(summary = "Создать запись на курс (идемпотентно по Idempotency-Key)")
    @PostMapping
    public Mono<ResponseEntity<EnrollmentResponse>> create(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody EnrollmentRequest request) {

        return enrollmentService.createEnrollment(idempotencyKey, request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @PostMapping("/test-rollback")
    public Mono<ResponseEntity<Map<String, String>>> testRollback(
            @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody EnrollmentRequest request) {
        return enrollmentService.createEnrollmentWithSimulatedFailure(key, request)
                .thenReturn(ResponseEntity.ok(Map.of("status", "should not happen")))
                .onErrorResume(ex -> Mono.just(ResponseEntity.status(500)
                        .body(Map.of("error", ex.getMessage()))));
    }
}