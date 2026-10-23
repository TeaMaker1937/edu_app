package edu.platform.education.notifications;

import java.util.Collection;
import java.util.Map;

/**
 * Публичный API модуля notifications.
 * Не принимает сущности из чужих модулей — только примитивы и их id.
 */
public interface NotificationService {

    void sendCourseCreatedEmail(Long teacherId, String teacherEmail, Long courseId, String courseTitle);

    void sendCustomNotification(Long userId, String userEmail, String subject, String body);

    void notifyEnrollment(Long userId, Long courseId, String courseTitle);

    long countByCourse(Long courseId);

    Map<Long, Long> countByCourseIds(Collection<Long> courseIds);
}