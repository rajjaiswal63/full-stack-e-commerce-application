package org.ecommerce.project.repository;

import org.ecommerce.project.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    java.util.Optional<Payment> findByPgPaymentId(String pgPaymentId);
    boolean existsByPgPaymentId(String pgPaymentId);
}
