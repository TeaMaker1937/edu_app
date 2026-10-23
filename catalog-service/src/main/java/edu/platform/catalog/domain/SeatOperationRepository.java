package edu.platform.catalog.domain;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface SeatOperationRepository extends ReactiveCrudRepository<SeatOperation, Long> {
    Mono<SeatOperation> findByCommandIdAndAction(String commandId, String action);
}