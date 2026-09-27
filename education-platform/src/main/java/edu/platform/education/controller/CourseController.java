package edu.platform.education.controller;

import edu.platform.education.dto.CourseRequest;
import edu.platform.education.dto.CourseResponse;
import edu.platform.education.entity.User;
import edu.platform.education.service.CourseService;
import edu.platform.education.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@Tag(name = "Курсы", description = "Создание и просмотр курсов")
public class CourseController {

    private final CourseService courseService;
    private final UserService userService;

    @Operation(summary = "Список всех курсов",
            description = "Доступно любой авторизованной роли.")
    @GetMapping
    public ResponseEntity<List<CourseResponse>> findAll() {
        return ResponseEntity.ok(courseService.findAll());
    }

    @Operation(summary = "Курс по идентификатору")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Курс найден"),
            @ApiResponse(responseCode = "404", description = "Курс не найден")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.findById(id));
    }

    @Operation(summary = "Создание курса",
            description = "Доступно только роли TEACHER. Текущий пользователь становится преподавателем курса.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Курс создан"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав (нужна роль TEACHER)")
    })
    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<CourseResponse> create(@Valid @RequestBody CourseRequest request,
                                                 @AuthenticationPrincipal UserDetails userDetails) {
        User teacher = userService.findByEmail(userDetails.getUsername());
        CourseResponse created = courseService.create(request, teacher.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}