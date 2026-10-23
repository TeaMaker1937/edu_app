package edu.platform.education.lessons;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findAllByCourseIdOrderById(Long courseId);

    /**
     * Плоский результат: курсы + уроки одним запросом.
     * Используется в «хорошем» варианте (без N+1).
     * Колонки: courseId, title, summary, price, teacherId,
     *          lessonId, lessonTitle, lessonVersion
     */
    @Query(value = """
            SELECT c.id          AS courseId,
                   c.title       AS title,
                   c.summary     AS summary,
                   c.price       AS price,
                   c.teacher_id  AS teacherId,
                   l.id          AS lessonId,
                   l.title       AS lessonTitle,
                   l.version     AS lessonVersion
            FROM courses c
            LEFT JOIN lessons l ON l.course_id = c.id
            ORDER BY c.id, l.id
            """, nativeQuery = true)
    List<Object[]> findAllCoursesWithLessonsFlat();
}