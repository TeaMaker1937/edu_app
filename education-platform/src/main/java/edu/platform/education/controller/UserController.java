package edu.platform.education.controller;

import edu.platform.education.dto.UserResponse;
import edu.platform.education.entity.User;
import edu.platform.education.mapper.UserMapper;
import edu.platform.education.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Пользователи", description = "Информация о текущем пользователе")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @Operation(summary = "Текущий пользователь",
            description = "Возвращает профиль пользователя, извлечённый из JWT-токена.")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(userMapper.toResponse(user));
    }
}