package edu.platform.catalog;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;

/**
 * Проверка чистой реактивной цепочки контроллера:
 * данные / пустой результат / ошибка.
 */
class CourseReactiveChainTest {

    private final CourseRepository repo = Mockito.mock(CourseRepository.class);
    private final CourseController controller = new CourseController(repo);

    @Test
    void whenData_thenEmit() {
        when(repo.findById(1L))
                .thenReturn(Mono.just(new Course(
                        1L, "A", "B", BigDecimal.ONE, 1L)));

        StepVerifier.create(controller.getOffer(1L, 0L))
                .assertNext(re -> {
                    // ResponseEntity<?> с кодом 200 и телом CourseOfferResponse
                    org.junit.jupiter.api.Assertions.assertEquals(200, re.getStatusCode().value());
                })
                .verifyComplete();
    }

    @Test
    void whenEmpty_then404() {
        when(repo.findById(999L)).thenReturn(Mono.empty());

        StepVerifier.create(controller.getOffer(999L, 0L))
                .assertNext(re ->
                        org.junit.jupiter.api.Assertions.assertEquals(404, re.getStatusCode().value()))
                .verifyComplete();
    }

    @Test
    void whenError_thenPropagates() {
        when(repo.findById(1L))
                .thenReturn(Mono.error(new RuntimeException("db fail")));

        StepVerifier.create(controller.getOffer(1L, 0L))
                .expectErrorMatches(ex -> ex instanceof RuntimeException
                        && ex.getMessage().equals("db fail"))
                .verify();
    }
}