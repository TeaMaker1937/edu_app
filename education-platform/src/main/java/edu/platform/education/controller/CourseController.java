package edu.platform.education.controller;

import edu.platform.education.dto.CourseRequest;
import edu.platform.education.dto.CourseResponse;
import edu.platform.education.entity.User;
import edu.platform.education.service.CourseService;
import edu.platform.education.service.UserService;
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
public class CourseController {

    private final CourseService courseService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<CourseResponse>> findAll() {
        return ResponseEntity.ok(courseService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<CourseResponse> create(@Valid @RequestBody CourseRequest request,
                                                 @AuthenticationPrincipal UserDetails userDetails) {
        User teacher = userService.findByEmail(userDetails.getUsername());
        CourseResponse created = courseService.create(request, teacher.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}