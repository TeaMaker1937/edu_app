package edu.platform.enrollment;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Счётчики успешных и ошибочных запросов предложений.
 * Теги: operation (имя операции) и result (success/error).
 * Не используем userId, courseId, полный URL — чтобы не раздувать cardinality.
 */
@Component
public class OfferMetrics {

    private static final String METRIC_NAME = "enrollment_offers_total";

    private final Counter successCounter;
    private final Counter errorCounter;

    public OfferMetrics(MeterRegistry registry) {
        this.successCounter = Counter.builder(METRIC_NAME)
                .description("Количество запросов на расчёт предложения записи")
                .tag("operation", "get_offer")
                .tag("result", "success")
                .register(registry);

        this.errorCounter = Counter.builder(METRIC_NAME)
                .description("Количество запросов на расчёт предложения записи")
                .tag("operation", "get_offer")
                .tag("result", "error")
                .register(registry);
    }

    public void recordSuccess() {
        successCounter.increment();
    }

    public void recordError() {
        errorCounter.increment();
    }
}