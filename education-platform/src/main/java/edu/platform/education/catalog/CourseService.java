package edu.platform.education.catalog;

import edu.platform.education.users.User;
import edu.platform.education.users.UserService;
import edu.platform.education.notifications.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

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
        course.setSummary(request.summary());
        Course saved = courseRepository.save(course);
        log.info("Создан курс '{}' (id={}) преподавателем {}", saved.getTitle(), saved.getId(), teacher.getEmail());

        notificationService.sendCourseCreatedEmail(
                teacher.getId(), teacher.getEmail(), saved.getId(), saved.getTitle());

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

    @Transactional(readOnly = true)
    public List<CourseReportResponse> getReport() {
        List<CourseReportProjection> base = courseRepository.getCourseReport();

        List<Long> ids = base.stream()
                .map(CourseReportProjection::getCourseId)
                .toList();

        Map<Long, Long> counts = notificationService.countByCourseIds(ids);

        return base.stream()
                .map(p -> new CourseReportResponse(
                        p.getCourseId(),
                        p.getTitle(),
                        p.getActiveCount(),
                        counts.getOrDefault(p.getCourseId(), 0L)))
                .toList();
    }
}