package edu.platform.education.notifications;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Счётчик отправок уведомлений.
 * Тег result: success / error.
 * Не используем userId, courseId, полный URL.
 */
@Component
public class NotificationMetrics {

    private static final String METRIC_NAME = "notification_sent_total";

    private final Counter successCounter;
    private final Counter errorCounter;

    public NotificationMetrics(MeterRegistry registry) {
        this.successCounter = Counter.builder(METRIC_NAME)
                .description("Количество отправленных уведомлений")
                .tag("result", "success")
                .register(registry);

        this.errorCounter = Counter.builder(METRIC_NAME)
                .description("Количество отправленных уведомлений")
                .tag("result", "error")
                .register(registry);
    }

    public void recordSuccess() { successCounter.increment(); }
    public void recordError()   { errorCounter.increment();   }
}