package edu.platform.education.controller;

import edu.platform.education.dto.LoginRequest;
import edu.platform.education.dto.UserRegistrationRequest;
import edu.platform.education.dto.UserResponse;
import edu.platform.education.entity.User;
import edu.platform.education.mapper.UserMapper;
import edu.platform.education.service.AuthService;
import edu.platform.education.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final UserMapper userMapper;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRegistrationRequest request) {
        User user = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toResponse(user));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginRequest request) {
        authService.authenticate(request); // проверяем креды
        // TODO: на этапе 1.1.8 здесь будет генерация JWT
        return ResponseEntity.ok(Map.of("token", "stub-token-will-be-replaced-with-jwt"));
    }
}