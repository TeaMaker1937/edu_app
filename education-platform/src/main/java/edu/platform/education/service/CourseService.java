package edu.platform.education.service;

import edu.platform.education.dto.CourseRequest;
import edu.platform.education.dto.CourseResponse;
import edu.platform.education.entity.Course;
import edu.platform.education.entity.User;
import edu.platform.education.mapper.CourseMapper;
import edu.platform.education.repository.CourseRepository;
import edu.platform.education.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;
    private final UserService userService;
    private final NotificationService notificationService;

    @Transactional
    public CourseResponse create(CourseRequest request, Long teacherId) {
        User teacher = userService.findById(teacherId);
        Course course = courseMapper.toEntity(request);
        course.setTeacher(teacher);
        Course saved = courseRepository.save(course);
        log.info("Создан курс '{}' (id={}) преподавателем {}", saved.getTitle(), saved.getId(), teacher.getEmail());

        // Уведомление преподавателю о создании курса
        notificationService.sendCourseCreatedEmail(teacher, saved);

        return courseMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> findAll() {
        return courseRepository.findAll().stream()
                .map(courseMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseResponse findById(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Курс с id " + id + " не найден"));
        return courseMapper.toResponse(course);
    }
}