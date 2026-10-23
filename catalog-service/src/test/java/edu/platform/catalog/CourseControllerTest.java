package edu.platform.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;

@WebFluxTest(controllers = CourseController.class)
class CourseControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CourseRepository courseRepository;

    @Test
    void whenCourseExists_then200() {
        when(courseRepository.findById(1L))
                .thenReturn(Mono.just(new Course(
                        1L, "C# Course", "Spring", new BigDecimal("1000.00"), 1L, 0)));

        webTestClient.get()
                .uri("/api/courses/1/offer")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.courseId").isEqualTo(1)
                .jsonPath("$.title").isEqualTo("C# Course");
    }

    @Test
    void whenCourseNotExists_then404() {
        when(courseRepository.findById(999L)).thenReturn(Mono.empty());

        webTestClient.get()
                .uri("/api/courses/999/offer")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("COURSE_NOT_FOUND");
    }
}