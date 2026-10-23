package edu.platform.enrollment.enrollments;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface IdempotencyKeyRepository extends ReactiveCrudRepository<IdempotencyKey, String> {
}