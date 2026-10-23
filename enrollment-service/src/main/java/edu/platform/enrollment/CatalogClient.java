package edu.platform.enrollment;

import edu.platform.enrollment.catalog.CatalogApi;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.bulkhead.operator.BulkheadOperator;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class CatalogClient {

    private static final Duration ATTEMPT_TIMEOUT = Duration.ofMillis(250);
    private static final Duration TOTAL_BUDGET = Duration.ofSeconds(1);
    private static final long MAX_RETRIES = 2;
    private static final Duration MIN_BACKOFF = Duration.ofMillis(100);
    private static final Duration MAX_BACKOFF = Duration.ofMillis(250);

    private final CatalogApi api;
    private final CircuitBreaker circuitBreaker;
    private final Bulkhead bulkhead;

    public CatalogClient(CatalogApi api,
                         CircuitBreakerRegistry cbRegistry,
                         BulkheadRegistry bulkheadRegistry) {
        this.api = api;
        this.circuitBreaker = cbRegistry.circuitBreaker("catalog");
        this.bulkhead = bulkheadRegistry.bulkhead("catalog");

        circuitBreaker.getEventPublisher()
                .onStateTransition(e -> log.warn("[CB] {} -> {}",
                        e.getStateTransition().getFromState(),
                        e.getStateTransition().getToState()));
    }

    public Mono<CourseOfferResponse> fetchOffer(Long courseId, String apiVersion) {
        return api.getOffer(courseId, apiVersion)
                .timeout(ATTEMPT_TIMEOUT)
                .retryWhen(buildRetrySpec())
                .timeout(TOTAL_BUDGET)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(BulkheadOperator.of(bulkhead))
                .onErrorMap(this::mapToDomainException);
    }

    public Mono<NotificationTemplateResponse> fetchNotificationTemplate(Long courseId, String apiVersion) {
        return api.getNotificationTemplate(courseId, apiVersion)
                .timeout(ATTEMPT_TIMEOUT)
                .retryWhen(buildRetrySpec())
                .timeout(TOTAL_BUDGET)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(BulkheadOperator.of(bulkhead))
                .onErrorMap(this::mapToDomainException);
    }

    private Retry buildRetrySpec() {
        return Retry.backoff(MAX_RETRIES, MIN_BACKOFF)
                .maxBackoff(MAX_BACKOFF)
                .jitter(0.0)
                .filter(this::isRetryable)
                .doBeforeRetry(signal ->
                        log.warn("Повтор #{} после ошибки: {}",
                                signal.totalRetries() + 1,
                                signal.failure().getMessage()))
                .onRetryExhaustedThrow((spec, signal) -> signal.failure());
    }

    private boolean isRetryable(Throwable t) {
        if (t instanceof WebClientRequestException) return true;
        if (t instanceof TimeoutException) return true;
        if (t instanceof WebClientResponseException ex) {
            return ex.getStatusCode().value() == 503;
        }
        return false;
    }

    private Throwable mapToDomainException(Throwable t) {
        if (t instanceof WebClientResponseException.NotFound notFound) {
            return new CourseNotFoundInCatalogException(extractCourseId(notFound));
        }
        if (t instanceof CallNotPermittedException) {
            return new CatalogUnavailableException(
                    "CircuitBreaker OPEN — запросы к catalog временно запрещены", t);
        }
        if (t instanceof BulkheadFullException) {
            return new CatalogUnavailableException(
                    "Bulkhead full — превышен лимит одновременных обращений", t);
        }
        if (t instanceof TimeoutException) {
            return new CatalogUnavailableException(
                    "Бюджет " + TOTAL_BUDGET + " исчерпан, catalog недоступен", t);
        }
        if (t instanceof WebClientResponseException ex) {
            return new CatalogUnavailableException(
                    "Catalog ответил ошибкой: " + ex.getStatusCode(), ex);
        }
        if (t instanceof WebClientRequestException ex) {
            return new CatalogUnavailableException(
                    "Catalog недоступен: " + ex.getMessage(), ex);
        }
        if (t instanceof CourseNotFoundInCatalogException
                || t instanceof CatalogUnavailableException) {
            return t;
        }
        return t;
    }

    private Long extractCourseId(WebClientResponseException ex) {
        String path = ex.getRequest().getURI().getPath();
        String[] parts = path.split("/");
        for (int i = parts.length - 1; i >= 0; i--) {
            if (!parts[i].isEmpty() && !"offer".equals(parts[i])
                    && !"notification-template".equals(parts[i])) {
                try { return Long.parseLong(parts[i]); } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }
}