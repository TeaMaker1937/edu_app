package edu.platform.enrollment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;

@WebFluxTest(controllers = EnrollmentOfferController.class)
@Import(GlobalExceptionHandler.class)
class EnrollmentOfferControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CatalogClient catalogClient;

    @Test
    void whenCatalogReturnsOffer_then200() {
        when(catalogClient.fetchOffer(1L))
                .thenReturn(Mono.just(new CourseOfferResponse(
                        1L, "C# Course", new BigDecimal("1000.00"))));

        webTestClient.get()
                .uri("/api/enrollment-offers?courseId=1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.courseId").isEqualTo(1)
                .jsonPath("$.title").isEqualTo("C# Course")
                .jsonPath("$.price").isEqualTo(1000.00);
    }

    @Test
    void whenCourseNotFound_then404() {
        when(catalogClient.fetchOffer(999L))
                .thenReturn(Mono.error(new CourseNotFoundInCatalogException(999L)));

        webTestClient.get()
                .uri("/api/enrollment-offers?courseId=999")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("COURSE_NOT_FOUND");
    }

    @Test
    void whenCatalogUnavailable_then503() {
        when(catalogClient.fetchOffer(1L))
                .thenReturn(Mono.error(new CatalogUnavailableException("unavailable", null)));

        webTestClient.get()
                .uri("/api/enrollment-offers?courseId=1")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.code").isEqualTo("CATALOG_UNAVAILABLE");
    }
}