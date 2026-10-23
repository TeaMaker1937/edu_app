package edu.platform.payment.messaging;

import edu.platform.payment.domain.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCommandListener {

    private final PaymentService paymentService;
    private final RabbitTemplate rabbit;

    @RabbitListener(queues = "payment.debit.queue")  public void onDebit(DebitCommand cmd)
    {
        log.info("[PAYMENT][CMD] debit commandId={}, enrollment={}, amount={}",
                cmd.commandId(), cmd.enrollmentId(), cmd.amount());

        PaymentService.DebitResult result = paymentService.debit(
                cmd.commandId(), cmd.enrollmentId(), cmd.userId(), cmd.amount());

        DebitedReply reply = new DebitedReply(
                cmd.commandId(), cmd.enrollmentId(),
                result.success(), result.reason());

        rabbit.convertAndSend(RabbitConfig.SAGA_EXCHANGE, RabbitConfig.RK_DEBITED, reply);
        log.info("[PAYMENT][REPLY] payment-debited commandId={}, success={}",
                cmd.commandId(), result.success());
    }

    @RabbitListener(queues = "payment.refund.queue") public void onRefund(RefundCommand cmd)
    {
        log.info("[PAYMENT][CMD] refund commandId={}, enrollment={}",
                cmd.commandId(), cmd.enrollmentId());

        PaymentService.RefundResult result = paymentService.refund(
                cmd.commandId(), cmd.enrollmentId());

        RefundedReply reply = new RefundedReply(
                cmd.commandId(), cmd.enrollmentId(),
                result.success(), result.reason());

        rabbit.convertAndSend(RabbitConfig.SAGA_EXCHANGE, RabbitConfig.RK_REFUNDED, reply);
        log.info("[PAYMENT][REPLY] payment-refunded commandId={}, success={}",
                cmd.commandId(), result.success());
    }
}