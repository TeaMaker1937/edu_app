package edu.platform.education.notifications.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE    = "enrollment.events";
    public static final String QUEUE       = "enrollment.created.queue";
    public static final String ROUTING_KEY = "enrollment.created";

    public static final String DLX        = "enrollment.dlx";
    public static final String DLQ        = "enrollment.created.dlq";
    public static final String DLQ_KEY    = "enrollment.created.dlq";

    public static final String CONFIRMED_QUEUE = "enrollment.confirmed.queue";
    public static final String RK_CONFIRMED    = "EnrollmentConfirmed";

    // ---- основной exchange + очередь + binding (без изменений) ----

    @Bean
    public TopicExchange enrollmentExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue enrollmentCreatedQueue() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(DLQ_KEY)
                .build();
    }

    @Bean
    public Binding enrollmentCreatedBinding(Queue enrollmentCreatedQueue,
                                            TopicExchange enrollmentExchange) {
        return BindingBuilder.bind(enrollmentCreatedQueue)
                .to(enrollmentExchange)
                .with(ROUTING_KEY);
    }

    // ---- DLX + DLQ ----

    @Bean
    public DirectExchange enrollmentDlx() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    public Queue enrollmentCreatedDlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding enrollmentCreatedDlqBinding(Queue enrollmentCreatedDlq,
                                               DirectExchange enrollmentDlx) {
        return BindingBuilder.bind(enrollmentCreatedDlq)
                .to(enrollmentDlx)
                .with(DLQ_KEY);
    }

    @Bean
    public Queue enrollmentConfirmedQueue() {
        return QueueBuilder.durable(CONFIRMED_QUEUE).build();
    }

    @Bean
    public Binding enrollmentConfirmedBinding(Queue enrollmentConfirmedQueue,
                                              TopicExchange enrollmentExchange) {
        return BindingBuilder.bind(enrollmentConfirmedQueue)
                .to(enrollmentExchange).with(RK_CONFIRMED);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(DefaultJackson2JavaTypeMapper.TypePrecedence.INFERRED);
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}