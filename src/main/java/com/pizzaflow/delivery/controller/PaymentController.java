package com.pizzaflow.delivery.controller;

import com.pizzaflow.common.ApiResponse;
import com.pizzaflow.delivery.dto.PaymentResponse;
import com.pizzaflow.delivery.dto.RefundRequest;
import com.pizzaflow.delivery.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getPaymentByOrderId(orderId)));
    }

    @PostMapping("/{orderId}/refund")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> refundPayment(
            @PathVariable Long orderId,
            @RequestBody(required = false) RefundRequest req) {
        String reason = (req != null && req.getReason() != null) ? req.getReason() : "Ручной возврат менеджером";
        paymentService.refundPayment(orderId, reason);
        return ResponseEntity.ok(ApiResponse.ok("Возврат оформлен", null));
    }
}
