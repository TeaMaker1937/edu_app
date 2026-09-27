package edu.platform.education.repository;

import edu.platform.education.entity.Course;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findAllByTeacherId(Long teacherId);

    // Пессимистическая блокировка: SELECT ... FOR UPDATE
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Course c WHERE c.id = :id")
    Optional<Course> findByIdForUpdate(@Param("id") Long id);
}