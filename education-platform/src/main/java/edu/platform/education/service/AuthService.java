package edu.platform.education.service;

import edu.platform.education.dto.LoginRequest;
import edu.platform.education.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Временная реализация: проверяет email и пароль, возвращает пользователя.
     * JWT-генерация будет добавлена на этапе настройки Security.
     */
    @Transactional(readOnly = true)
    public User authenticate(LoginRequest request) {
        User user = userService.findByEmail(request.email());
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new RuntimeException("Неверный пароль"); // заменим на BadCredentialsException
        }
        return user;
    }
}