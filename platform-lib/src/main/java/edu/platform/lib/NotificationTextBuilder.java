package edu.platform.lib;

public final class NotificationTextBuilder {

    private NotificationTextBuilder() {}

    public static String buildEnrollmentMessage(String courseTitle) {
        if (courseTitle == null || courseTitle.isBlank()) {
            throw new IllegalArgumentException("Course title must not be blank");
        }
        return "Вы записаны на курс " + courseTitle;
    }
}