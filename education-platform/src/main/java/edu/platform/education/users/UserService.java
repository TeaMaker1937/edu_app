package edu.platform.education.users;

import edu.platform.education.shared.exception.ResourceNotFoundException;
import edu.platform.education.shared.exception.UserAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;

    @Transactional
    public User register(UserRegistrationRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("Пользователь с email " + request.email() + " уже существует");
        }
        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        User saved = userRepository.save(user);
        log.info("Зарегистрирован новый пользователь: {}", saved.getEmail());

        // Уведомление отправит модуль notifications по событию
        events.publishEvent(new UserRegisteredEvent(
                saved.getId(), saved.getEmail(), saved.getFullName(), saved.getRole()));

        return saved;
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь с email " + email + " не найден"));
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь с id " + id + " не найден"));
    }
}