package edu.platform.education.notifications;

import edu.platform.education.users.User;
import edu.platform.education.users.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Уведомления", description = "Модуль уведомлений (вариант 4)")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    @Operation(summary = "Отправить тестовое уведомление текущему пользователю")
    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> testNotification(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "Тестовое уведомление") String subject,
            @RequestParam(defaultValue = "Это проверка сервиса уведомлений.") String body) {

        User user = userService.findByEmail(userDetails.getUsername());
        notificationService.sendCustomNotification(user.getId(), user.getEmail(), subject, body);

        return ResponseEntity.ok(Map.of(
                "status", "SENT",
                "channel", "log",
                "recipient", user.getEmail()
        ));
    }

    @Operation(summary = "Число уведомлений курса (показатель варианта)")
    @GetMapping("/courses/{courseId}/count")
    public ResponseEntity<Map<String, Long>> countByCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(Map.of("notificationCount", notificationService.countByCourse(courseId)));
    }
}