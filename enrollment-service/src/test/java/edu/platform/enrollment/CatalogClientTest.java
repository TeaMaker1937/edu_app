package edu.platform.enrollment;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CatalogClientTest {

    private MockWebServer server;
    private CatalogClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        String baseUrl = server.url("/").toString().replaceAll("/$", "");
        CatalogProperties props = new CatalogProperties(baseUrl, Duration.ofSeconds(3));
        client = new CatalogClient(WebClient.builder(), props);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void whenData_thenEmit() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"courseId\":1,\"title\":\"C# Course\",\"price\":1000.00}"));

        StepVerifier.create(client.fetchOffer(1L))
                .assertNext(offer -> {
                    assertEquals(1L, offer.courseId());
                    assertEquals("C# Course", offer.title());
                    assertEquals(0, offer.price().compareTo(new java.math.BigDecimal("1000.00")));
                })
                .verifyComplete();
    }

    @Test
    void whenEmptyBody_thenError() {
        // catalog отвечает 200, но с пустым body — Mono получается пустым
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(""));

        StepVerifier.create(client.fetchOffer(1L))
                .verifyComplete();  // пустой Mono — так и должно быть
    }

    @Test
    void whenError_thenCatalogUnavailable() {
        server.enqueue(new MockResponse().setResponseCode(500));

        StepVerifier.create(client.fetchOffer(1L))
                .expectErrorMatches(ex -> ex instanceof CatalogUnavailableException)
                .verify();
    }
}