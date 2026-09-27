package edu.platform.education.controller;

import edu.platform.education.entity.User;
import edu.platform.education.service.UserService;
import edu.platform.education.service.notification.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Уведомления", description = "Заглушечный сервис уведомлений (пишет в лог)")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    @Operation(summary = "Отправить тестовое уведомление текущему пользователю",
            description = "Демонстрирует работу NotificationService. В логе появится запись вида «📧 [NOTIFICATION] ...»")
    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> testNotification(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "Тестовое уведомление") String subject,
            @RequestParam(defaultValue = "Это проверка работы сервиса уведомлений.") String body) {

        User user = userService.findByEmail(userDetails.getUsername());
        notificationService.sendCustomNotification(user, subject, body);

        return ResponseEntity.ok(Map.of(
                "status", "SENT",
                "channel", "log",
                "recipient", user.getEmail()
        ));
    }
}