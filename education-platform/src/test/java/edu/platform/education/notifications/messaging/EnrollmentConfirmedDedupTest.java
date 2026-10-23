package edu.platform.education.notifications.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.platform.education.notifications.NotificationService;
import edu.platform.education.notifications.inbox.InboxRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Защищает вариант 4 (уведомления) от регрессии:
 *  - при первом событии — создаётся ровно одна notification, метрика SKIP не растёт
 *  - при повторной доставке того же eventId — notification НЕ создаётся, метрика SKIP растёт
 *
 * Если кто-то снова сделает HANDLER_ID уникальным при каждом запуске
 * (System.currentTimeMillis) — tryInsert вернёт 1 (мок default? нет — test
 * на это не сработает, но второй тест тогда упадёт по missing verify counter.increment()).
 */
@ExtendWith(MockitoExtension.class)
class EnrollmentConfirmedDedupTest {

    @Mock NotificationService notificationService;
    @Mock InboxRepository inboxRepository;
    @Mock ObjectMapper objectMapper;
    @Mock MeterRegistry meterRegistry;
    @Mock Counter counter;

    EnrollmentConfirmedListener listener;

    @BeforeEach
    void setUp() {
        listener = new EnrollmentConfirmedListener(
                notificationService, inboxRepository, objectMapper, meterRegistry);
    }

    @Test
    void whenEventFirstTime_thenNotificationCreated() throws Exception {
        EnrollmentConfirmedEvent event = new EnrollmentConfirmedEvent(
                "evt-1", 42L, 1L, 2L, "C# Course", LocalDateTime.now());

        when(objectMapper.writeValueAsString(event)).thenReturn("{}");
        when(inboxRepository.tryInsert(eq("evt-1"), eq("notification"), anyString()))
                .thenReturn(1);

        listener.onConfirmed(event);

        verify(notificationService).notifyEnrollment(2L, 1L, "C# Course");
        // При первом событии listener НЕ должен даже пытаться взять counter
        verify(meterRegistry, never()).counter(anyString(), anyString(), anyString());
    }

    @Test
    void whenEventDuplicate_thenNotificationNotCreated_andSkipMetricIncremented() throws Exception {
        EnrollmentConfirmedEvent event = new EnrollmentConfirmedEvent(
                "evt-1", 42L, 1L, 2L, "C# Course", LocalDateTime.now());

        when(objectMapper.writeValueAsString(event)).thenReturn("{}");
        when(inboxRepository.tryInsert(eq("evt-1"), eq("notification"), anyString()))
                .thenReturn(0);   // ← дубликат
        when(meterRegistry.counter(anyString(), anyString(), anyString())).thenReturn(counter);

        listener.onConfirmed(event);

        verify(notificationService, never()).notifyEnrollment(any(), any(), any());
        verify(counter).increment();
    }
}