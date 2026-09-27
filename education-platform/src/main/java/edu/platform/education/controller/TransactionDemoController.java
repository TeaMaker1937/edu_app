package edu.platform.education.controller;

import edu.platform.education.dto.CourseResponse;
import edu.platform.education.service.TransactionDemoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/demo/transactions")
@RequiredArgsConstructor
@Tag(name = "Демонстрация транзакций",
        description = "Показывает разные режимы транзакций и блокировки (для защиты лабы)")
public class TransactionDemoController {

    private final TransactionDemoService demoService;

    @Operation(summary = "Переименовать курс с пессимистической блокировкой (только TEACHER)")
    @PostMapping("/pessimistic/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<String> pessimistic(@PathVariable Long id, @RequestParam String title) {
        demoService.renameCoursePessimistic(id, title);
        return ResponseEntity.ok("OK: пессимистическая блокировка отработала");
    }

    @Operation(summary = "Переименовать курс с оптимистической блокировкой (только TEACHER)")
    @PostMapping("/optimistic/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<String> optimistic(@PathVariable Long id, @RequestParam String title) {
        demoService.renameCourseOptimistic(id, title);
        return ResponseEntity.ok("OK: оптимистическая блокировка отработала");
    }

    @Operation(summary = "Чтение с уровнем READ_COMMITTED")
    @GetMapping("/read-committed/{id}")
    public ResponseEntity<CourseResponse> readCommitted(@PathVariable Long id) {
        return ResponseEntity.ok(demoService.readCommitted(id));
    }

    @Operation(summary = "Чтение с уровнем REPEATABLE_READ")
    @GetMapping("/repeatable-read/{id}")
    public ResponseEntity<CourseResponse> repeatableRead(@PathVariable Long id) {
        return ResponseEntity.ok(demoService.repeatableRead(id));
    }

    @Operation(summary = "Чтение с уровнем SERIALIZABLE")
    @GetMapping("/serializable/{id}")
    public ResponseEntity<CourseResponse> serializable(@PathVariable Long id) {
        return ResponseEntity.ok(demoService.serializable(id));
    }
}