package edu.platform.education.dto;

import edu.platform.education.entity.Role;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        Role role
) {}