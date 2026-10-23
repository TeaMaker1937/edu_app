package edu.platform.enrollment;

import edu.platform.enrollment.catalog.CatalogApi;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogClientTest {

    @Mock
    CatalogApi catalogApi;

    private CatalogClient client;

    @BeforeEach
    void setUp() {
        CircuitBreakerRegistry cb = CircuitBreakerRegistry.ofDefaults();
        BulkheadRegistry bh = BulkheadRegistry.ofDefaults();
        client = new CatalogClient(catalogApi, cb, bh);
    }

    @Test
    void whenData_thenEmit() {
        when(catalogApi.getOffer(1L, "1.0"))
                .thenReturn(Mono.just(new CourseOfferResponse(
                        1L, "C# Course", new BigDecimal("1000.00"))));

        StepVerifier.create(client.fetchOffer(1L, "1.0"))
                .assertNext(offer -> {
                    assertEquals(1L, offer.courseId());
                    assertEquals("C# Course", offer.title());
                    assertEquals(0, offer.price().compareTo(new BigDecimal("1000.00")));
                })
                .verifyComplete();
    }

    @Test
    void whenEmpty_thenEmpty() {
        when(catalogApi.getOffer(1L, "1.0")).thenReturn(Mono.empty());

        StepVerifier.create(client.fetchOffer(1L, "1.0"))
                .verifyComplete();
    }

    @Test
    void whenError_thenCatalogUnavailable() {
        // 500 — не retryable, сразу идёт в mapToDomainException → CatalogUnavailable
        when(catalogApi.getOffer(1L, "1.0"))
                .thenReturn(Mono.error(new WebClientResponseException(
                        500, "Internal Server Error", null, null, null)));

        StepVerifier.create(client.fetchOffer(1L, "1.0"))
                .expectError(CatalogUnavailableException.class)
                .verify();
    }
}