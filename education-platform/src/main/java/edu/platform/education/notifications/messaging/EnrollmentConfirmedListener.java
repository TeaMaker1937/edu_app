package edu.platform.education.notifications.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.platform.education.notifications.NotificationService;
import edu.platform.education.notifications.inbox.InboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnrollmentConfirmedListener {
    private static final String HANDLER_ID = "notification";
    //private static final String HANDLER_ID = "notification-no-inbox-" + System.currentTimeMillis();

    private final NotificationService notificationService;
    private final InboxRepository inboxRepository;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitConfig.CONFIRMED_QUEUE)
    @Transactional
    public void onConfirmed(EnrollmentConfirmedEvent event) {
        log.info("[CONFIRMED-CONSUMER] Получено: eventId={}, enrollmentId={}, courseId={}, userId={}",
                event.eventId(), event.enrollmentId(), event.courseId(), event.userId());

        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            log.error("[CONFIRMED-CONSUMER] Не удалось сериализовать event: {}", e.getMessage());
            throw new IllegalStateException("Bad event payload", e);
        }

        // inbox: атомарная проверка + запись, дедуп по (eventId, handlerId)
        int inserted = inboxRepository.tryInsert(event.eventId(), HANDLER_ID, payload);
        if (inserted == 0) {
            log.warn("[CONFIRMED-CONSUMER] Дубликат eventId={}, handler={} — SKIP",
                    event.eventId(), HANDLER_ID);
            return;
        }

        notificationService.notifyEnrollment(
                event.userId(), event.courseId(), event.courseTitle());

        log.info("[CONFIRMED-CONSUMER] Notification создана eventId={}, enrollmentId={}",
                event.eventId(), event.enrollmentId());
    }
}