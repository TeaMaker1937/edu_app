package edu.platform.catalog.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String SAGA_EXCHANGE = "saga.events";

    public static final String RESERVE_QUEUE = "catalog.reserve.queue";
    public static final String RELEASE_QUEUE = "catalog.release.queue";

    public static final String RK_RESERVE = "cmd.reserve-seat";
    public static final String RK_RELEASE = "cmd.release-seat";

    public static final String RK_RESERVED = "reply.seat-reserved";
    public static final String RK_RELEASED = "reply.seat-released";

    @Bean
    public TopicExchange sagaExchange() {
        return new TopicExchange(SAGA_EXCHANGE, true, false);
    }

    @Bean
    public Queue catalogReserveQueue() {
        return QueueBuilder.durable(RESERVE_QUEUE).build();
    }

    @Bean
    public Queue catalogReleaseQueue() {
        return QueueBuilder.durable(RELEASE_QUEUE).build();
    }

    @Bean
    public Binding reserveBinding(Queue catalogReserveQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(catalogReserveQueue).to(sagaExchange).with(RK_RESERVE);
    }

    @Bean
    public Binding releaseBinding(Queue catalogReleaseQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(catalogReleaseQueue).to(sagaExchange).with(RK_RELEASE);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}