package edu.platform.education.controller;

import edu.platform.education.dto.LoginRequest;
import edu.platform.education.dto.UserRegistrationRequest;
import edu.platform.education.dto.UserResponse;
import edu.platform.education.entity.User;
import edu.platform.education.mapper.UserMapper;
import edu.platform.education.service.AuthService;
import edu.platform.education.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Аутентификация", description = "Регистрация и вход в систему")
@SecurityRequirements  // отключает требование Bearer-токена для всех методов этого контроллера
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final UserMapper userMapper;

    @Operation(summary = "Регистрация нового пользователя",
            description = "Создаёт пользователя с указанной ролью (STUDENT / TEACHER / ADMIN). Пароль шифруется BCrypt.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Пользователь зарегистрирован"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "409", description = "Email уже занят")
    })
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRegistrationRequest request) {
        User user = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toResponse(user));
    }

    @Operation(summary = "Вход в систему",
            description = "Проверяет email и пароль, возвращает JWT-токен.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Токен успешно выдан"),
            @ApiResponse(responseCode = "401", description = "Неверные учётные данные")
    })
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.authenticate(request);
        return ResponseEntity.ok(Map.of("token", token));
    }
}