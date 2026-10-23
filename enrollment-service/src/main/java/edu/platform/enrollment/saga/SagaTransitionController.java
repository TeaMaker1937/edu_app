package edu.platform.enrollment.saga;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/saga")
@RequiredArgsConstructor
public class SagaTransitionController {

    private final SagaInstanceRepository sagaRepo;
    private final SagaTransitionRepository transitionRepo;

    @GetMapping("/{sagaId}")
    public Mono<SagaInstance> get(@PathVariable Long sagaId) {
        return sagaRepo.findById(sagaId);
    }

    @GetMapping("/by-enrollment/{enrollmentId}")
    public Mono<SagaInstance> byEnrollment(@PathVariable Long enrollmentId) {
        return sagaRepo.findByEnrollmentId(enrollmentId);
    }

    @GetMapping("/{sagaId}/transitions")
    public Flux<SagaTransition> transitions(@PathVariable Long sagaId) {
        return transitionRepo.findBySagaIdOrderByIdAsc(sagaId);
    }
}