package edu.platform.enrollment;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;   // ← добавить эту строку
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/enrollment-offers")
public class EnrollmentOfferController {

    private final CatalogClient catalogClient;
    private final OfferMetrics metrics;

    public EnrollmentOfferController(CatalogClient catalogClient, OfferMetrics metrics) {
        this.catalogClient = catalogClient;
        this.metrics = metrics;
    }

    @GetMapping
    public Mono<CourseOfferResponse> getOffer(@RequestParam Long courseId) {
        return catalogClient.fetchOffer(courseId, "1.0")
                .doOnSuccess(r -> metrics.recordSuccess())
                .doOnError(e -> metrics.recordError());
    }

    @GetMapping("/{courseId}/notification-template")
    public Mono<NotificationTemplateResponse> getNotificationTemplate(@PathVariable Long courseId) {
        return catalogClient.fetchNotificationTemplate(courseId, "1.0")
                .doOnSuccess(r -> metrics.recordSuccess())
                .doOnError(e -> metrics.recordError());
    }
}