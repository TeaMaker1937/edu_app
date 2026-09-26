package edu.platform.education.dto;

import edu.platform.education.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRegistrationRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6, message = "Пароль должен содержать минимум 6 символов") String password,
        @NotBlank String fullName,
        @NotNull Role role
) {}