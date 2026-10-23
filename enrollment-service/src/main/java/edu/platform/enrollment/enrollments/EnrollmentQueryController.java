package edu.platform.enrollment.enrollments;

import edu.platform.enrollment.CatalogClient;
import edu.platform.enrollment.CourseOfferResponse;
import edu.platform.enrollment.NotificationTemplateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentQueryController {

    private static final String V1 = "1.0";
    private static final String V2 = "2.0";
    private static final Duration AGGREGATE_TIMEOUT = Duration.ofSeconds(1);

    private final EnrollmentRepository enrollmentRepo;
    private final CatalogClient catalogClient;

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Object>> get(
            @PathVariable Long id,
            @RequestHeader(value = "API-Version", required = false) String apiVersion) {

        if (apiVersion == null || apiVersion.isBlank()) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(Map.of(
                            "code", "API_VERSION_REQUIRED",
                            "message", "Заголовок API-Version обязателен. Поддерживается: 1.0, 2.0"
                    )));
        }
        if (!V1.equals(apiVersion) && !V2.equals(apiVersion)) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(Map.of(
                            "code", "API_VERSION_UNSUPPORTED",
                            "message", "Неподдерживаемая версия: " + apiVersion + ". Поддерживается: 1.0, 2.0"
                    )));
        }

        return enrollmentRepo.findById(id)
                .flatMap(enrollment -> {
                    if (V1.equals(apiVersion)) {
                        // Приводим на уровне аргумента — ResponseEntity.ok(Object) выведет ResponseEntity<Object>
                        return Mono.just(ResponseEntity.ok((Object) toV1(enrollment)));
                    }
                    return buildV2(enrollment, apiVersion)
                            .<ResponseEntity<Object>>map(ResponseEntity::ok);
                })
                .onErrorResume(ex -> {
                    log.warn("[V2] ошибка агрегации для id={}: {}", id, ex.getMessage());
                    return Mono.just(ResponseEntity.status(503)
                            .<Object>body(Map.of(
                                    "code", "CATALOG_UNAVAILABLE",
                                    "message", "Сервис catalog временно недоступен"
                            )));
                })
                .defaultIfEmpty(ResponseEntity.status(404)
                        .<Object>body(Map.of(
                                "code", "ENROLLMENT_NOT_FOUND",
                                "message", "Запись с id " + id + " не найдена"
                        )));
    }

    private EnrollmentV1Response toV1(Enrollment e) {
        return new EnrollmentV1Response(
                e.getId(), e.getCourseId(), e.getUserId(),
                e.getCourseTitle(), e.getPrice(), e.getStatus(), e.getCreatedAt());
    }

    private Mono<EnrollmentV2Response> buildV2(Enrollment e, String apiVersion) {
        // offer — критичный источник. Ошибка → 503 (обрабатывается в get()).
        Mono<CourseOfferResponse> offerMono =
                catalogClient.fetchOffer(e.getCourseId(), apiVersion);

        Mono<Optional<String>> templateMono =
                catalogClient.fetchNotificationTemplate(e.getCourseId(), apiVersion)
                        .map(NotificationTemplateResponse::template)
                        .map(Optional::of)
                        .onErrorResume(ex -> {
                            log.warn("[V2] variant-источник недоступен для courseId={}: {}",
                                    e.getCourseId(), ex.getMessage());
                            return Mono.just(Optional.empty());
                        });

        return Mono.zip(offerMono, templateMono)
                .timeout(AGGREGATE_TIMEOUT)
                .map(tuple -> {
                    CourseOfferResponse offer = tuple.getT1();
                    Optional<String> template = tuple.getT2();
                    return new EnrollmentV2Response(
                            e.getId(), e.getCourseId(), e.getUserId(),
                            e.getCourseTitle(), e.getPrice(), e.getStatus(), e.getCreatedAt(),
                            new EnrollmentV2Response.CourseOfferView(
                                    offer.courseId(), offer.title(), offer.price()),
                            template.orElse(null),
                            template.isPresent()
                    );
                });
    }
}