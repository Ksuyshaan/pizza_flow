package com.pizzaflow.delivery.service;

import com.pizzaflow.common.exception.ResourceNotFoundException;
import com.pizzaflow.delivery.dto.PaymentResponse;
import com.pizzaflow.delivery.entity.Payment;
import com.pizzaflow.delivery.entity.PaymentStatus;
import com.pizzaflow.delivery.repository.PaymentRepository;
import com.pizzaflow.report.service.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AuditService auditService;

    public PaymentService(PaymentRepository paymentRepository, AuditService auditService) {
        this.paymentRepository = paymentRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Платеж для заказа " + orderId + " не найден"));
        return PaymentResponse.fromEntity(payment);
    }

    @Transactional
    public void markPaymentPaid(Long orderId) {
        paymentRepository.findByOrderId(orderId).ifPresent(p -> {
            if (p.getPaymentStatus() != PaymentStatus.PAID) {
                p.setPaymentStatus(PaymentStatus.PAID);
                p.setPaidAt(Instant.now());
                if (p.getTransactionReference() == null) {
                    p.setTransactionReference("CASH-COLLECTED-" + System.currentTimeMillis());
                }
                paymentRepository.save(p);
                auditService.log("SYSTEM", "PAYMENT", "PAYMENT_PAID",
                        "Заказ #" + p.getOrder().getOrderNumber() + " успешно оплачен (" + p.getAmount() + " руб., " + p.getPaymentMethod() + ")");
            }
        });
    }

    @Transactional
    public void refundPayment(Long orderId, String reason) {
        paymentRepository.findByOrderId(orderId).ifPresent(p -> {
            if (p.getPaymentStatus() == PaymentStatus.PAID) {
                p.setPaymentStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(p);
                auditService.log("SYSTEM", "PAYMENT", "PAYMENT_REFUNDED",
                        "Возврат средств по заказу #" + p.getOrder().getOrderNumber() + " (" + p.getAmount() + " руб.): " + reason);
            }
        });
    }
}
