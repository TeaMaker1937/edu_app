package edu.platform.enrollment.enrollments;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface EnrollmentRepository extends ReactiveCrudRepository<Enrollment, Long> {
}