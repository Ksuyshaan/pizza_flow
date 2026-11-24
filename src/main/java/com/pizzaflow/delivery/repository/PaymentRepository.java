package com.pizzaflow.delivery.repository;

import com.pizzaflow.delivery.entity.Payment;
import com.pizzaflow.delivery.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(Long orderId);
    List<Payment> findByPaymentStatus(PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.paymentStatus = 'PAID' AND p.paidAt >= :from AND p.paidAt <= :to")
    BigDecimal sumRevenueBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT p FROM Payment p WHERE p.paymentStatus = 'PAID' AND p.paidAt >= :from AND p.paidAt <= :to")
    List<Payment> findPaidBetween(@Param("from") Instant from, @Param("to") Instant to);
}
