package edu.platform.education.catalog;

import edu.platform.education.users.User;
import edu.platform.education.users.UserService;
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

import org.springframework.data.domain.Page;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@Tag(name = "Курсы", description = "Создание и просмотр курсов")
public class CourseController {

    private final CourseService courseService;
    private final UserService userService;
    private final CourseSearchService courseSearchService;

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

    @Operation(summary = "Отчёт по курсам",
            description = "courseId, title, activeCount и notificationCount (показатель варианта 4).")
    @GetMapping("/report")
    public ResponseEntity<List<CourseReportResponse>> report() {
        return ResponseEntity.ok(courseService.getReport());
    }

    @Operation(summary = "Поиск курсов через Criteria API",
            description = "Все параметры необязательны. Сортировка: price ASC, id ASC.")
    @GetMapping("/search/criteria")
    public ResponseEntity<Page<CourseResponse>> searchCriteria(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(courseSearchService.searchByCriteria(title, minPrice, maxPrice, page, size));
    }

    @Operation(summary = "Поиск курсов через Specification",
            description = "Все параметры необязательны. Сортировка: price ASC, id ASC.")
    @GetMapping("/search/spec")
    public ResponseEntity<Page<CourseResponse>> searchSpec(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(courseSearchService.searchBySpecification(title, minPrice, maxPrice, page, size));
    }

    @Operation(summary = "Изменение курса (TEACHER — только свой, ADMIN — любой)")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<CourseResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CourseUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(courseService.update(id, request, currentUser));
    }
}