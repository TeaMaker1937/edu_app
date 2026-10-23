package edu.platform.payment.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByCommandId(String commandId);
    boolean existsByCommandId(String commandId);

    Optional<Payment> findFirstByEnrollmentIdAndStatusOrderByIdDesc(
            Long enrollmentId, PaymentStatus status);
}