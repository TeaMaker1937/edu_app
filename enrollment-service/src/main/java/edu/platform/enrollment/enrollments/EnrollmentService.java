package edu.platform.enrollment.enrollments;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.platform.enrollment.CatalogClient;
import edu.platform.enrollment.CourseOfferResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;
import edu.platform.enrollment.messaging.EnrollmentEvent;
import edu.platform.enrollment.outbox.OutboxEvent;
import edu.platform.enrollment.outbox.OutboxEventRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final IdempotencyKeyRepository idempotencyRepository;
    private final CatalogClient catalogClient;
    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;

    // ---------- Существующий ключ ----------

    private Mono<EnrollmentResponse> handleExisting(IdempotencyKey existing, String requestHash) {
        if (!existing.getRequestHash().equals(requestHash)) {
            return Mono.error(new IdempotencyConflictException(
                    "Idempotency-Key '" + existing.getIdempotencyKey()
                            + "' уже использован с другим телом запроса"));
        }
        log.info("Повторный запрос с Idempotency-Key={}, возвращаю сохранённый ответ",
                existing.getIdempotencyKey());
        try {
            return Mono.just(objectMapper.readValue(existing.getResponseBody(),
                    EnrollmentResponse.class));
        } catch (Exception e) {
            return Mono.error(new IllegalStateException(
                    "Не удалось десериализовать сохранённый ответ", e));
        }
    }

    // ---------- Новый ключ ----------

    private Mono<EnrollmentResponse> processNew(String key, String requestHash, EnrollmentRequest req) {
        return catalogClient.fetchOffer(req.courseId(), "1.0")   // ← "1.0"
                .flatMap(offer -> persist(key, requestHash, req, offer));
    }

    @Transactional
    public Mono<EnrollmentResponse> createEnrollment(String key, EnrollmentRequest request) {
        String requestHash = sha256(request.courseId() + ":" + request.userId());

        return idempotencyRepository.findById(key)
                .flatMap(existing -> handleExisting(existing, requestHash))
                .switchIfEmpty(Mono.defer(() -> processNew(key, requestHash, request)))
                .onErrorResume(DuplicateKeyException.class, ex -> {
                    log.warn("Гонка Idempotency-Key={}, читаю существующую запись", key);
                    return idempotencyRepository.findById(key)
                            .flatMap(existing -> handleExisting(existing, requestHash));
                });
    }

    private Mono<EnrollmentResponse> persist(String key, String requestHash,
                                             EnrollmentRequest req, CourseOfferResponse offer) {
        String eventId = UUID.randomUUID().toString();

        Enrollment toSave = Enrollment.builder()
                .idempotencyKey(key)
                .userId(req.userId())
                .courseId(offer.courseId())
                .courseTitle(offer.title())
                .price(offer.price())
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();

        return enrollmentRepository.save(toSave)
                .flatMap(saved -> {
                    EnrollmentResponse response = new EnrollmentResponse(
                            saved.getId(), saved.getCourseId(), saved.getUserId(),
                            saved.getCourseTitle(), saved.getPrice(), saved.getStatus(),
                            saved.getCreatedAt(), eventId);

                    String body;
                    try {
                        body = objectMapper.writeValueAsString(response);
                    } catch (Exception e) {
                        return Mono.error(new IllegalStateException("Не удалось сериализовать ответ", e));
                    }

                    IdempotencyKey idKey = IdempotencyKey.builder()
                            .idempotencyKey(key)
                            .requestHash(requestHash)
                            .responseStatus(201)
                            .responseBody(body)
                            .createdAt(LocalDateTime.now())
                            .build();

                    // === OUTBOX: сериализуем событие и сохраняем в БД в той же транзакции ===
                    String eventPayload;
                    try {
                        EnrollmentEvent evt = new EnrollmentEvent(
                                eventId, saved.getId(), saved.getCourseId(),
                                saved.getUserId(), LocalDateTime.now());
                        eventPayload = objectMapper.writeValueAsString(evt);
                    } catch (Exception e) {
                        return Mono.error(new IllegalStateException("Не удалось сериализовать event", e));
                    }

                    OutboxEvent outbox = OutboxEvent.builder()
                            .eventId(eventId)
                            .eventType("EnrollmentCreated")
                            .aggregateId(saved.getId())
                            .payload(eventPayload)
                            .createdAt(LocalDateTime.now())
                            .attempts(0)
                            .build();

                    return idempotencyRepository.save(idKey)
                            .then(outboxEventRepository.save(outbox))
                            .thenReturn(response);
                });
    }

    @Transactional
    public Mono<EnrollmentResponse> createEnrollmentWithSimulatedFailure(
            String key, EnrollmentRequest request) {
        return createEnrollment(key, request)
                .flatMap(r -> Mono.error(new RuntimeException("СИМУЛЯЦИЯ СБОЯ после всех save")));
    }

    // ---------- Утилита ----------

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 недоступен", e);
        }
    }
}