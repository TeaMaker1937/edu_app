package edu.platform.enrollment.saga;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface SagaTransitionRepository extends ReactiveCrudRepository<SagaTransition, Long> {
    Flux<SagaTransition> findBySagaIdOrderByIdAsc(Long sagaId);
}