package edu.platform.catalog;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CourseRepository extends ReactiveCrudRepository<Course, Long> {

    Flux<Course> findAllByOrderByIdAsc();

    Mono<Course> findByTitle(String title);
}