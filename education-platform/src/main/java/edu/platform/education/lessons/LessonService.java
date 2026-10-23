package edu.platform.education.lessons;

import edu.platform.education.catalog.CourseResponse;
import edu.platform.education.catalog.CourseService;
import edu.platform.education.shared.exception.ResourceNotFoundException;
import edu.platform.education.shared.exception.VersionConflictException;
import edu.platform.education.users.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;
    private final CourseService courseService;
    private final LessonMapper lessonMapper;

    // ---------- Публичный API ----------

    @Transactional
    public LessonResponse create(Long courseId, LessonCreateRequest request, Long userId, Role role) {
        ensureCanModifyCourse(courseId, userId, role);

        Lesson lesson = Lesson.builder()
                .courseId(courseId)
                .title(request.title())
                .content(request.content())
                .version(0L)
                .build();

        Lesson saved = lessonRepository.save(lesson);
        log.info("Создан урок '{}' (id={}) курса {}", saved.getTitle(), saved.getId(), courseId);
        return lessonMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public LessonResponse findById(Long id) {
        return lessonMapper.toResponse(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<LessonResponse> findByCourse(Long courseId) {
        return lessonRepository.findAllByCourseIdOrderById(courseId).stream()
                .map(lessonMapper::toResponse)
                .toList();
    }

    @Transactional
    public LessonResponse update(Long id, LessonUpdateRequest request, Long userId, Role role) {
        Lesson lesson = getOrThrow(id);
        ensureCanModifyCourse(lesson.getCourseId(), userId, role);

        // Проверка optimistic lock вручную, чтобы отдать красивую ошибку
        if (!lesson.getVersion().equals(request.version())) {
            throw new VersionConflictException(
                    "Конфликт версий: у вас " + request.version()
                            + ", в БД " + lesson.getVersion()
                            + ". Перечитайте урок и повторите.");
        }

        lesson.setTitle(request.title());
        lesson.setContent(request.content());
        // version инкрементирует Hibernate при flush
        return lessonMapper.toResponse(lesson);
    }

    // ---------- Приватное ----------

    private Lesson getOrThrow(Long id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Урок с id " + id + " не найден"));
    }

    private void ensureCanModifyCourse(Long courseId, Long userId, Role role) {
        if (role == Role.ADMIN) return;
        if (role != Role.TEACHER) {
            throw new AccessDeniedException("Только TEACHER или ADMIN могут изменять уроки");
        }
        CourseResponse course = courseService.findById(courseId);
        if (!course.teacherId().equals(userId)) {
            throw new AccessDeniedException("Преподаватель может изменять уроки только своих курсов");
        }
    }
}