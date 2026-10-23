package edu.platform.enrollment.catalog;

import edu.platform.enrollment.CourseOfferResponse;
import edu.platform.enrollment.NotificationTemplateResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import reactor.core.publisher.Mono;

@HttpExchange(url = "/api/courses")
public interface CatalogApi {
    @GetExchange("/{id}/offer")
    Mono<CourseOfferResponse> getOffer(
            @PathVariable("id") Long id,
            @RequestHeader("API-Version") String apiVersion);

    @GetExchange("/{id}/notification-template")
    Mono<NotificationTemplateResponse> getNotificationTemplate(
            @PathVariable("id") Long id,
            @RequestHeader("API-Version") String apiVersion);
}