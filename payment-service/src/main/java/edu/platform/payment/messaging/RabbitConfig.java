package edu.platform.payment.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String SAGA_EXCHANGE        = "saga.events";
    public static final String PAYMENT_DEBIT_QUEUE  = "payment.debit.queue";
    public static final String PAYMENT_REFUND_QUEUE = "payment.refund.queue";

    public static final String RK_DEBIT  = "cmd.debit-payment";
    public static final String RK_REFUND = "cmd.refund-payment";

    public static final String RK_DEBITED  = "reply.payment-debited";
    public static final String RK_REFUNDED = "reply.payment-refunded";

    @Bean
    public TopicExchange sagaExchange() {
        return new TopicExchange(SAGA_EXCHANGE, true, false);
    }

    @Bean
    public Queue paymentDebitQueue()  { return QueueBuilder.durable(PAYMENT_DEBIT_QUEUE).build(); }

    @Bean
    public Queue paymentRefundQueue() { return QueueBuilder.durable(PAYMENT_REFUND_QUEUE).build(); }

    @Bean
    public Binding debitBinding(Queue paymentDebitQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(paymentDebitQueue).to(sagaExchange).with(RK_DEBIT);
    }

    @Bean
    public Binding refundBinding(Queue paymentRefundQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(paymentRefundQueue).to(sagaExchange).with(RK_REFUND);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}