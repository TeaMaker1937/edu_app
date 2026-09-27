package edu.platform.education.service.notification;

import edu.platform.education.entity.Course;
import edu.platform.education.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Заглушечная реализация сервиса уведомлений.
 * Пишет «отправленные письма» в лог приложения.
 */
@Slf4j
@Service
public class LoggingNotificationService implements NotificationService {

    @Override
    public void sendWelcomeEmail(User user) {
        log.info("📧 [NOTIFICATION] {} — Отправлено приветственное письмо на {} (пользователь: {}, роль: {})",
                LocalDateTime.now(), user.getEmail(), user.getFullName(), user.getRole());
    }

    @Override
    public void sendCourseCreatedEmail(User teacher, Course course) {
        log.info("📧 [NOTIFICATION] {} — Отправлено письмо преподавателю {} о создании курса '{}' (id={})",
                LocalDateTime.now(), teacher.getEmail(), course.getTitle(), course.getId());
    }

    @Override
    public void sendCustomNotification(User user, String subject, String body) {
        log.info("📧 [NOTIFICATION] {} — Отправлено письмо на {}: тема='{}', текст='{}'",
                LocalDateTime.now(), user.getEmail(), subject, body);
    }
}