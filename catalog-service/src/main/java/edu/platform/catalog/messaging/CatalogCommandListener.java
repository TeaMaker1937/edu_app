package edu.platform.catalog.messaging;

import edu.platform.catalog.domain.SeatReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogCommandListener {

    private final SeatReservationService seats;
    private final RabbitTemplate rabbit;

    @RabbitListener(queues = RabbitConfig.RESERVE_QUEUE)
    public void onReserve(ReserveSeatCommand cmd) {
        log.info("[CATALOG][CMD] reserve commandId={}, enrollment={}, course={}",
                cmd.commandId(), cmd.enrollmentId(), cmd.courseId());

        seats.reserve(cmd).subscribe(reply -> {
            rabbit.convertAndSend(RabbitConfig.SAGA_EXCHANGE,
                    RabbitConfig.RK_RESERVED, reply);
            log.info("[CATALOG][REPLY] seat-reserved commandId={}, success={}",
                    cmd.commandId(), reply.success());
        });
    }

    @RabbitListener(queues = RabbitConfig.RELEASE_QUEUE)
    public void onRelease(ReleaseSeatCommand cmd) {
        log.info("[CATALOG][CMD] release commandId={}, enrollment={}, course={}",
                cmd.commandId(), cmd.enrollmentId(), cmd.courseId());

        seats.release(cmd).subscribe(reply -> {
            rabbit.convertAndSend(RabbitConfig.SAGA_EXCHANGE,
                    RabbitConfig.RK_RELEASED, reply);
            log.info("[CATALOG][REPLY] seat-released commandId={}, success={}",
                    cmd.commandId(), reply.success());
        });
    }
}