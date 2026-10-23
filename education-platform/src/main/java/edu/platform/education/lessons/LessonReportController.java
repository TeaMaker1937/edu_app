package edu.platform.education.lessons;

import edu.platform.education.catalog.CourseResponse;
import edu.platform.education.catalog.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/lessons-report")
@Tag(name = "Отчёт по курсам с уроками (N+1 демо)")
public class LessonReportController {

    private final CourseService courseService;      // публичный API модуля catalog
    private final LessonRepository lessonRepository; // package-private, живём в том же модуле

    public LessonReportController(CourseService courseService,
                                  LessonRepository lessonRepository) {
        this.courseService = courseService;
        this.lessonRepository = lessonRepository;
    }

    /**
     * ПЛОХОЙ вариант: N+1.
     * 1 запрос за курсами + N запросов за уроками каждого курса.
     */
    @Operation(summary = "Плохой вариант: N+1")
    @GetMapping("/bad")
    public List<CourseWithLessonsResponse> bad() {
        List<CourseResponse> courses = courseService.findAll();   // 1 запрос
        return courses.stream()
                .map(c -> new CourseWithLessonsResponse(
                        c.id(),
                        c.title(),
                        c.summary(),
                        c.price(),
                        c.teacherId(),
                        lessonRepository.findAllByCourseIdOrderById(c.id()).stream()   // +1 запрос на курс
                                .map(l -> new CourseWithLessonsResponse.LessonSummary(
                                        l.getId(), l.getTitle(), l.getVersion()))
                                .toList()
                ))
                .toList();
    }

    /**
     * ХОРОШИЙ вариант: один SQL с JOIN.
     */
    @Operation(summary = "Хороший вариант: один запрос")
    @GetMapping("/good")
    public List<CourseWithLessonsResponse> good() {
        List<Object[]> flat = lessonRepository.findAllCoursesWithLessonsFlat();

        // Группируем строки по courseId
        Map<Long, List<Object[]>> grouped = flat.stream()
                .collect(Collectors.groupingBy(
                        row -> ((Number) row[0]).longValue(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        return grouped.entrySet().stream()
                .map(e -> {
                    Object[] first = e.getValue().get(0);
                    List<CourseWithLessonsResponse.LessonSummary> lessons = e.getValue().stream()
                            .filter(row -> row[5] != null)
                            .map(row -> new CourseWithLessonsResponse.LessonSummary(
                                    ((Number) row[5]).longValue(),
                                    (String) row[6],
                                    ((Number) row[7]).longValue()))
                            .toList();

                    return new CourseWithLessonsResponse(
                            e.getKey(),
                            (String) first[1],
                            (String) first[2],
                            (BigDecimal) first[3],
                            ((Number) first[4]).longValue(),
                            lessons);
                })
                .toList();
    }
}