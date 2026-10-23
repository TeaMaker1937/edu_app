package edu.platform.enrollment.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /** Штатное событие EnrollmentCreated (оставляем для обратной совместимости с лабой 4). */
    public Mono<Void> publishEnrollmentCreated(EnrollmentEvent event) {
        String json;
        try {
            json = new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(event);
        } catch (Exception e) {
            return Mono.error(e);
        }
        return publishRaw(
                RabbitConfig.ROUTING_KEY,
                event.eventId(),
                json,
                "edu.platform.enrollment.messaging.EnrollmentEvent");
    }

    /**
     * Публикация уже сериализованного JSON.
     *
     * Используем rabbitTemplate.send(Message) вместо convertAndSend, потому что
     * Jackson2JsonMessageConverter обернул бы String в кавычки — получилось бы
     * "{\"a\":1}" вместо {"a":1}. Шлём сырые байты и явно проставляем
     * __TypeId__ по routing key, чтобы consumer знал целевой класс.
     */
    public Mono<Void> publishRaw(String routingKey, String eventId,
                                 String jsonPayload, String typeId) {
        return Mono.create(sink -> {
            String exchange = routingKey.startsWith("cmd.")
                    ? RabbitConfig.SAGA_EXCHANGE
                    : RabbitConfig.EXCHANGE;

            log.info("[PUBLISH] Начало eventId={}, exchange={}, routingKey={}, typeId={}",
                    eventId, exchange, routingKey, typeId);

            MessageProperties props = new MessageProperties();
            props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            props.setContentEncoding("UTF-8");
            props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            props.setMessageId(eventId);
            if (typeId != null && !typeId.isBlank()) {
                props.setHeader("__TypeId__", typeId);
            }

            Message message = new Message(jsonPayload.getBytes(StandardCharsets.UTF_8), props);
            CorrelationData cd = new CorrelationData(eventId);

            try {
                rabbitTemplate.send(exchange, routingKey, message, cd);
            } catch (Exception ex) {
                log.error("[PUBLISH] Ошибка send: {}", ex.getMessage(), ex);
                sink.error(ex);
                return;
            }

            cd.getFuture().whenComplete((confirm, throwable) -> {
                if (throwable != null) {
                    log.error("[PUBLISH] throwable: {}", throwable.getMessage(), throwable);
                    sink.error(throwable);
                    return;
                }
                if (confirm == null || !confirm.isAck()) {
                    log.error("[PUBLISH] NACK eventId={}", eventId);
                    sink.error(new IllegalStateException("Брокер не подтвердил (NACK)"));
                    return;
                }
                if (cd.getReturned() != null) {
                    String reason = cd.getReturned().getReplyText();
                    log.error("[PUBLISH] Возврат NO_ROUTE eventId={}, reply={}",
                            eventId, reason);
                    sink.error(new IllegalStateException(
                            "Не доставлено ни в одну очередь: reply='" + reason + "'"));
                    return;
                }
                log.info("[PUBLISH] Успех eventId={}, routingKey={}", eventId, routingKey);
                sink.success();
            });
        });
    }
}