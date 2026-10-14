package edu.platform.education.lessons;

import edu.platform.education.users.User;
import edu.platform.education.users.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
@Tag(name = "Уроки", description = "Модуль уроков (lab 2, п.12)")
public class LessonController {

    private final LessonService lessonService;
    private final UserService userService;

    @Operation(summary = "Создать урок в курсе (TEACHER своего курса / ADMIN любого)")
    @PostMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<LessonResponse> create(@PathVariable Long courseId,
                                                 @Valid @RequestBody LessonCreateRequest request,
                                                 @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        LessonResponse created = lessonService.create(courseId, request, user.getId(), user.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Урок по id")
    @GetMapping("/{id}")
    public ResponseEntity<LessonResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(lessonService.findById(id));
    }

    @Operation(summary = "Уроки курса")
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<LessonResponse>> findByCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(lessonService.findByCourse(courseId));
    }

    @Operation(summary = "Изменить урок (проверка версии → 409)")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<LessonResponse> update(@PathVariable Long id,
                                                 @Valid @RequestBody LessonUpdateRequest request,
                                                 @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(lessonService.update(id, request, user.getId(), user.getRole()));
    }
}