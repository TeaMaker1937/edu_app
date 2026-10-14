package edu.platform.education.users;

public record UserRegisteredEvent(
        Long userId,
        String email,
        String fullName,
        Role role
) {}