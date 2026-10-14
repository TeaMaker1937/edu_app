package edu.platform.enrollment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Публичный клиент к catalog-service.
 * Все ошибки приводятся к нашим исключениям:
 *   - 404 от catalog → CourseNotFoundInCatalogException
 *   - 5xx, timeout, connection refused → CatalogUnavailableException
 */
@Slf4j
@Component
public class CatalogClient {

    private final WebClient webClient;
    private final CatalogProperties properties;

    public CatalogClient(WebClient.Builder builder, CatalogProperties properties) {
        this.properties = properties;
        this.webClient = builder
                .baseUrl(properties.baseUrl())
                .build();
    }

    public Mono<CourseOfferResponse> fetchOffer(Long courseId) {
        return webClient.get()
                .uri("/api/courses/{id}/offer", courseId)
                .retrieve()
                .bodyToMono(CourseOfferResponse.class)
                .onErrorMap(WebClientResponseException.NotFound.class,
                        ex -> new CourseNotFoundInCatalogException(courseId))
                .onErrorMap(WebClientResponseException.class,
                        ex -> new CatalogUnavailableException(
                                "Catalog ответил ошибкой: " + ex.getStatusCode(), ex))
                .onErrorMap(WebClientRequestException.class,
                        ex -> new CatalogUnavailableException(
                                "Catalog недоступен: " + ex.getMessage(), ex));
    }

    public Mono<NotificationTemplateResponse> fetchNotificationTemplate(Long courseId) {
        return webClient.get()
                .uri("/api/courses/{id}/notification-template", courseId)
                .retrieve()
                .bodyToMono(NotificationTemplateResponse.class)
                .onErrorMap(WebClientResponseException.NotFound.class,
                        ex -> new CourseNotFoundInCatalogException(courseId))
                .onErrorMap(WebClientResponseException.class,
                        ex -> new CatalogUnavailableException(
                                "Catalog ответил ошибкой: " + ex.getStatusCode(), ex))
                .onErrorMap(WebClientRequestException.class,
                        ex -> new CatalogUnavailableException(
                                "Catalog недоступен: " + ex.getMessage(), ex));
    }
}