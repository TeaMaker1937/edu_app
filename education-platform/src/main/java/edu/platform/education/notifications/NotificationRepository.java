package edu.platform.education.notifications;

import org.springframework.data.jpa.repository.JpaRepository;

interface NotificationRepository extends JpaRepository<Notification, Long> {

    long countByCourseId(Long courseId);
}