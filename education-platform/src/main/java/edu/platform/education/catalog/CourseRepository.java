package edu.platform.education.catalog;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {
    List<Course> findAllByTeacherId(Long teacherId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Course c WHERE c.id = :id")
    Optional<Course> findByIdForUpdate(@Param("id") Long id);

    /**
     * Отчёт по курсам: id, title и число ACTIVE-записей.
     * LEFT JOIN + COALESCE дают activeCount = 0 для курсов без записей.
     */
    @Query(value = """
            SELECT c.id                    AS courseId,
                   c.title                 AS title,
                   COALESCE(COUNT(e.id), 0) AS activeCount
            FROM courses c
            LEFT JOIN enrollments e
                   ON e.course_id = c.id
                  AND e.status = 'ACTIVE'
            GROUP BY c.id, c.title
            ORDER BY c.id
            """, nativeQuery = true)
    List<CourseReportProjection> getCourseReport();
}