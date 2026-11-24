package com.pizzaflow.delivery.dto;

import com.pizzaflow.delivery.entity.Payment;
import com.pizzaflow.delivery.entity.PaymentMethod;
import com.pizzaflow.delivery.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public class PaymentResponse {
    private Long id;
    private Long orderId;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private BigDecimal amount;
    private String transactionReference;
    private Instant paidAt;

    public PaymentResponse() {
    }

    public static PaymentResponse fromEntity(Payment payment) {
        PaymentResponse res = new PaymentResponse();
        res.setId(payment.getId());
        if (payment.getOrder() != null) {
            res.setOrderId(payment.getOrder().getId());
        }
        res.setPaymentMethod(payment.getPaymentMethod());
        res.setPaymentStatus(payment.getPaymentStatus());
        res.setAmount(payment.getAmount());
        res.setTransactionReference(payment.getTransactionReference());
        res.setPaidAt(payment.getPaidAt());
        return res;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }
}
