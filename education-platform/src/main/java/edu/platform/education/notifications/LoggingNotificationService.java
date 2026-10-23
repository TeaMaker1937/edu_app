package edu.platform.education.notifications;

import edu.platform.education.users.UserRegisteredEvent;
import edu.platform.lib.NotificationTextBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
class LoggingNotificationService implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMetrics metrics;

    /**
     * Реакция на регистрацию пользователя.
     * Модуль notifications подписан на событие из модуля users —
     * зависимость users → notifications отсутствует.
     */
    @EventListener
    void onUserRegistered(UserRegisteredEvent event) {
        try {
            log.info("📧 [NOTIFICATION] {} — Приветственное письмо на {} (id={}, роль={})",
                    LocalDateTime.now(), event.email(), event.userId(), event.role());
            metrics.recordSuccess();
        } catch (RuntimeException e) {
            metrics.recordError();
            throw e;
        }
    }

    @Override
    public void sendCourseCreatedEmail(Long teacherId, String teacherEmail,
                                       Long courseId, String courseTitle) {
        log.info("📧 [NOTIFICATION] {} — Письмо преподавателю {} о курсе '{}' (id={})",
                LocalDateTime.now(), teacherEmail, courseTitle, courseId);
    }

    @Override
    public void sendCustomNotification(Long userId, String userEmail, String subject, String body) {
        log.info("📧 [NOTIFICATION] {} — Письмо на {}: тема='{}', текст='{}'",
                LocalDateTime.now(), userEmail, subject, body);
    }

    @Override
    @Transactional
    public void notifyEnrollment(Long userId, Long courseId, String courseTitle) {
        try {
            String message = NotificationTextBuilder.buildEnrollmentMessage(courseTitle);
            Notification n = Notification.builder()
                    .userId(userId)
                    .courseId(courseId)
                    .message(message)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(n);
            log.info("📧 [NOTIFICATION] Сохранено уведомление: userId={}, courseId={}, message='{}'",
                    userId, courseId, message);
            metrics.recordSuccess();
        } catch (RuntimeException e) {
            log.error("Ошибка отправки уведомления: {}", e.getMessage());
            metrics.recordError();
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long countByCourse(Long courseId) {
        return notificationRepository.countByCourseId(courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> countByCourseIds(Collection<Long> courseIds) {
        Map<Long, Long> result = new HashMap<>();
        for (Long id : courseIds) {
            result.put(id, notificationRepository.countByCourseId(id));
        }
        return result;
    }
}