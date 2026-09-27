package edu.platform.education.service.notification;

import edu.platform.education.entity.Course;
import edu.platform.education.entity.User;

/**
 * Сервис уведомлений.
 * В текущей реализации — заглушка, пишущая в лог.
 * В продакшене здесь может быть JavaMail, SendGrid, Kafka-продюсер и т.д.
 */
public interface NotificationService {

    void sendWelcomeEmail(User user);

    void sendCourseCreatedEmail(User teacher, Course course);

    void sendCustomNotification(User user, String subject, String body);
}