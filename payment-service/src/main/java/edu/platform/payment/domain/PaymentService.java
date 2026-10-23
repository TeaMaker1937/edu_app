package edu.platform.payment.domain;

import edu.platform.payment.config.PaymentModeHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository repo;
    private final PaymentModeHolder modeHolder;
    @Value("${payment.refund.enabled:true}")
    private boolean refundEnabled;

    /**
     * Идемпотентное списание.
     * Возвращает true, если деньги списаны (или уже были списаны с этим commandId).
     * Возвращает false, если режим REJECT или случился бизнес-отказ.
     */
    @Transactional
    public DebitResult debit(String commandId, Long enrollmentId, Long userId, BigDecimal amount) {
        // 1. Уже обрабатывали этот commandId?
        Optional<Payment> existing = repo.findByCommandId(commandId);
        if (existing.isPresent()) {
            Payment p = existing.get();
            log.info("[PAYMENT] idempotent debit: commandId={}, status={}", commandId, p.getStatus());
            return new DebitResult(p.getStatus() == PaymentStatus.DEBITED,
                    p.getStatus() == PaymentStatus.DEBITED ? null : "already refunded");
        }

        // 2. Какой режим?
        PaymentMode mode = modeHolder.get();
        log.info("[PAYMENT] debit: commandId={}, mode={}, amount={}", commandId, mode, amount);

        if (mode == PaymentMode.DELAY) {
            int seconds = modeHolder.delaySeconds();
            log.info("[PAYMENT] DELAY активен — сплю {} сек перед обработкой", seconds);
            try { Thread.sleep(seconds * 1000L); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            // После паузы всё равно успех — сценарий "поздний ответ"
        }

        if (mode == PaymentMode.REJECT) {
            // Не создаём запись: отказ — это не платёж.
            log.info("[PAYMENT] debit rejected by mode, commandId={}", commandId);
            return new DebitResult(false, "rejected by payment mode");
        }

        // 3. Успех: сохраняем
        Payment saved = repo.save(Payment.builder()
                .commandId(commandId)
                .enrollmentId(enrollmentId)
                .userId(userId)
                .amount(amount)
                .status(PaymentStatus.DEBITED)
                .createdAt(OffsetDateTime.now())
                .build());
        log.info("[PAYMENT] DEBITED: id={}, commandId={}, amount={}",
                saved.getId(), commandId, amount);
        return new DebitResult(true, null);
    }

    /**
     * Идемпотентный возврат.
     * Ищем платёж по enrollmentId (не по commandId refund'а):
     * — если есть DEBITED → переводим в REFUNDED;
     * — если нет → холостой успех (либо уже вернули, либо нечего возвращать).
     * commandId refund'а используется только для лога и как будущий ключ дедупликации.
     */
    @Transactional
    public RefundResult refund(String refundCommandId, Long enrollmentId) {
        if (!refundEnabled) {
            log.warn("[PAYMENT] REFUND DISABLED — возврат не выполняется, commandId={}",
                    refundCommandId);
            return new RefundResult(false, "refund disabled");
        }
        Optional<Payment> p = repo.findFirstByEnrollmentIdAndStatusOrderByIdDesc(
                enrollmentId, PaymentStatus.DEBITED);
        if (p.isEmpty()) {
            log.info("[PAYMENT] refund: активного DEBITED для enrollmentId={} нет — холостой, commandId={}",
                    enrollmentId, refundCommandId);
            return new RefundResult(true, "nothing to refund");
        }
        Payment payment = p.get();
        payment.setStatus(PaymentStatus.REFUNDED);
        log.info("[PAYMENT] REFUNDED: enrollmentId={}, refundCommandId={}, amount={}",
                enrollmentId, refundCommandId, payment.getAmount());
        return new RefundResult(true, null);
    }

    public record DebitResult(boolean success, String reason) {}
    public record RefundResult(boolean success, String reason) {}
}