package edu.platform.enrollment.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String SAGA_EXCHANGE = "saga.events";   // ← НОВЫЙ exchange для Saga
    public static final String REPLY_QUEUE = "enrollment.reply.queue";
    public static final String RK_SEAT_RESERVED = "reply.seat-reserved";
    public static final String RK_SEAT_RELEASED = "reply.seat-released";
    public static final String RK_PAYMENT_DEBITED = "reply.payment-debited";
    public static final String RK_PAYMENT_REFUNDED = "reply.payment-refunded";

    public static final String EXCHANGE = "enrollment.events";
    public static final String QUEUE = "enrollment.created.queue";
    public static final String ROUTING_KEY = "enrollment.created";

    public static final String DLX = "enrollment.dlx";
    public static final String DLQ = "enrollment.created.dlq";
    public static final String DLQ_KEY = "enrollment.created.dlq";

    @Bean
    public TopicExchange enrollmentExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    // ВАЖНО: те же DLX-аргументы, что и в platform, иначе PRECONDITION_FAILED
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
    public Jackson2JsonMessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        // INFERRED = не смотрим на __TypeId__, listener сам решит, что парсить
        typeMapper.setTypePrecedence(DefaultJackson2JavaTypeMapper.TypePrecedence.INFERRED);
        typeMapper.setTrustedPackages("*");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf,
                                         Jackson2JsonMessageConverter converter) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(converter);
        t.setMandatory(true);
        t.setReturnsCallback(returned -> System.out.printf(
                "[RETURN] reply=%s, exchange=%s, routingKey=%s%n",
                returned.getReplyText(), returned.getExchange(), returned.getRoutingKey()));
        return t;
    }

    @Bean
    public TopicExchange sagaExchange() {
        return new TopicExchange(SAGA_EXCHANGE, true, false);
    }

    @Bean
    public Queue enrollmentReplyQueue() {
        return QueueBuilder.durable(REPLY_QUEUE).build();
    }

    @Bean
    public Binding seatReservedBinding(Queue enrollmentReplyQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(enrollmentReplyQueue).to(sagaExchange).with(RK_SEAT_RESERVED);
    }

    @Bean
    public Binding seatReleasedBinding(Queue enrollmentReplyQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(enrollmentReplyQueue).to(sagaExchange).with(RK_SEAT_RELEASED);
    }

    @Bean
    public Binding paymentDebitedBinding(Queue enrollmentReplyQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(enrollmentReplyQueue).to(sagaExchange).with(RK_PAYMENT_DEBITED);
    }

    @Bean
    public Binding paymentRefundedBinding(Queue enrollmentReplyQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(enrollmentReplyQueue).to(sagaExchange).with(RK_PAYMENT_REFUNDED);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory replyListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(new SimpleMessageConverter());
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}