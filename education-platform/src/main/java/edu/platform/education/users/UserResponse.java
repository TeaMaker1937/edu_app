package edu.platform.education.users;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        Role role
) {}