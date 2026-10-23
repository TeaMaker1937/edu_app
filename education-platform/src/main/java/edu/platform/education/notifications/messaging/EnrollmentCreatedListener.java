package edu.platform.education.notifications.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.platform.education.notifications.NotificationService;
import edu.platform.education.notifications.inbox.InboxRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
public class EnrollmentCreatedListener {

    private static final String HANDLER_NOTIFICATION = "notification";
    private static final String HANDLER_AUDIT        = "audit";

    private final NotificationService notificationService;
    private final InboxRepository inboxRepository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate tx;

    public EnrollmentCreatedListener(NotificationService notificationService,
                                     InboxRepository inboxRepository,
                                     ObjectMapper objectMapper,
                                     PlatformTransactionManager tm) {
        this.notificationService = notificationService;
        this.inboxRepository = inboxRepository;
        this.objectMapper = objectMapper;
        this.tx = new TransactionTemplate(tm);
        this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void onEnrollmentCreated(EnrollmentEvent event) {
        log.info("[CONSUMER] Получено: eventId={}, enrollmentId={}, courseId={}, userId={}",
                event.eventId(), event.enrollmentId(), event.courseId(), event.userId());

        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (Exception ex) {
            log.error("[CONSUMER] Не удалось сериализовать eventId={}: {}",
                    event.eventId(), ex.getMessage());
            throw new IllegalStateException("Bad event payload", ex);
        }

        // ====================== Handler 1: NOTIFICATION ======================
        Boolean notificationProcessed = tx.execute(status -> {
            int inserted = inboxRepository.tryInsert(
                    event.eventId(), HANDLER_NOTIFICATION, payload);

            if (inserted == 0) {
                log.warn("[HANDLER {}] Дубликат eventId={} — SKIP",
                        HANDLER_NOTIFICATION, event.eventId());
                return false;
            }

            log.info("[HANDLER {}] Inbox создан eventId={}",
                    HANDLER_NOTIFICATION, event.eventId());
            notificationService.notifyEnrollment(
                    event.userId(), event.courseId(), "#" + event.courseId());
            log.info("[HANDLER {}] Notification сохранена eventId={}",
                    HANDLER_NOTIFICATION, event.eventId());
            return true;
        });
        log.info("[HANDLER {}] COMMIT прошёл, processed={}",
                HANDLER_NOTIFICATION, notificationProcessed);

        // ====================== Handler 2: AUDIT ======================
        Boolean auditProcessed = tx.execute(status -> {
            int inserted = inboxRepository.tryInsert(
                    event.eventId(), HANDLER_AUDIT, payload);

            if (inserted == 0) {
                log.warn("[HANDLER {}] Дубликат eventId={} — SKIP",
                        HANDLER_AUDIT, event.eventId());
                return false;
            }

            log.info("[HANDLER {}] Inbox создан eventId={}",
                    HANDLER_AUDIT, event.eventId());
            log.info("[AUDIT] Обработано событие eventId={}, enrollmentId={}, userId={}",
                    event.eventId(), event.enrollmentId(), event.userId());
            return true;
        });
        log.info("[HANDLER {}] COMMIT прошёл, processed={}",
                HANDLER_AUDIT, auditProcessed);

        // ===== Контроль: данные реально в БД? =====
        long inDbNotification = inboxRepository.countByEventIdAndHandlerId(
                event.eventId(), HANDLER_NOTIFICATION);
        long inDbAudit = inboxRepository.countByEventIdAndHandlerId(
                event.eventId(), HANDLER_AUDIT);
        log.info("[POST-COMMIT] inbox notification={}, inbox audit={}",
                inDbNotification, inDbAudit);

        // ====================== ПАУЗА после обоих commit, до ACK ======================
        if (Boolean.parseBoolean(System.getenv("PAUSE_AFTER_COMMIT"))) {
            log.warn("[CONSUMER] PAUSE_AFTER_COMMIT активен — оба commit прошли, " +
                    "но ACK ещё НЕ отправлен. Убей контейнер сейчас!");
            try {
                Thread.sleep(300_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        log.info("[CONSUMER] Возврат из listener — Spring AMQP сделает ACK eventId={} " +
                        "(notification={}, audit={})",
                event.eventId(), notificationProcessed, auditProcessed);
    }
}