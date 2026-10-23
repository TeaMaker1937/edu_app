package edu.platform.catalog;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.math.BigDecimal;

import edu.platform.lib.NotificationTextBuilder;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseRepository courseRepository;

    public CourseController(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    /**
     * Предложение записи на курс (без создания записи).
     * GET /api/courses/{id}/offer
     * 200: { courseId, title, price }
     * 404: { code: "COURSE_NOT_FOUND", message: ... }
     */

    @GetMapping("/{id}/offer")
    public Mono<ResponseEntity<Object>> getOffer(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "0") long delayMs) {

        // ==== ЛАБА 4, п.2: спец. id для проверки retry-фильтра в enrollment ====
        if (id == 400L) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(Map.of("code", "BAD_REQUEST")));
        }
        if (id == 401L) {
            return Mono.just(ResponseEntity.status(401)
                    .body(Map.of("code", "UNAUTHORIZED")));
        }
        if (id == 403L) {
            return Mono.just(ResponseEntity.status(403)
                    .body(Map.of("code", "FORBIDDEN")));
        }
        if (id == 500L) {
            return Mono.just(ResponseEntity.ok(
                    new CourseOfferResponse(500L, "Variant Test Course", BigDecimal.ZERO)));
        }
        if (id == 503L) {
            return Mono.just(ResponseEntity.status(503)
                    .body(Map.of("code", "SERVICE_UNAVAILABLE")));
        }
        // ==== /спец. id ====

        Mono<Course> courseMono = courseRepository.findById(id);
        if (delayMs > 0) {
            courseMono = courseMono.delayElement(Duration.ofMillis(delayMs));
        }

        return courseMono
                .<ResponseEntity<Object>>map(c -> ResponseEntity.ok(
                        new CourseOfferResponse(c.getId(), c.getTitle(), c.getPrice())))
                .defaultIfEmpty(ResponseEntity.status(404).body(Map.of(
                        "code", "COURSE_NOT_FOUND",
                        "message", "Курс с id " + id + " не найден"
                )));
    }

    @GetMapping("/{id}/notification-template")
    public Mono<ResponseEntity<Object>> getNotificationTemplate(@PathVariable Long id) {
        if (id == 500L) {
            return Mono.just(ResponseEntity.status(500).body(Map.of("code", "INTERNAL")));
        }
        return courseRepository.findById(id)
                .<ResponseEntity<Object>>map(c -> ResponseEntity.ok(
                        new NotificationTemplateResponse(
                                c.getId(),
                                c.getTitle(),
                                NotificationTextBuilder.buildEnrollmentMessage(c.getTitle()))))
                .defaultIfEmpty(ResponseEntity.status(404).body(Map.of(
                        "code", "COURSE_NOT_FOUND",
                        "message", "Курс с id " + id + " не найден"
                )));
    }
}