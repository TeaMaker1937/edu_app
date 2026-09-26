package edu.platform.education.repository;

import edu.platform.education.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findAllByTeacherId(Long teacherId);
}