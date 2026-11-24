package com.pizzaflow.order.controller;

import com.pizzaflow.common.ApiResponse;
import com.pizzaflow.common.PageResponse;
import com.pizzaflow.order.dto.CancelOrderRequest;
import com.pizzaflow.order.dto.CreateOrderRequest;
import com.pizzaflow.order.dto.OrderResponse;
import com.pizzaflow.order.dto.StatusHistoryResponse;
import com.pizzaflow.order.entity.OrderStatus;
import com.pizzaflow.order.entity.UserRole;
import com.pizzaflow.order.service.OrderService;
import com.pizzaflow.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateOrderRequest req) {
        OrderResponse res = orderService.createOrder(req, principal.getId(), principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Заказ успешно оформлен", res));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getOrders(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long filterUserId = null;
        if (principal.getRole() == UserRole.CUSTOMER) {
            filterUserId = principal.getId(); // Customers can only see their own orders!
        }

        Page<OrderResponse> result = orderService.getOrders(
                filterUserId,
                status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        PageResponse<OrderResponse> pageResponse = new PageResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );
        return ResponseEntity.ok(ApiResponse.ok(pageResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long id) {
        OrderResponse res = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        OrderResponse res = orderService.changeStatus(
                id,
                OrderStatus.CONFIRMED,
                principal.getUsername(),
                principal.getRole(),
                "Заказ подтвержден менеджером"
        );
        return ResponseEntity.ok(ApiResponse.ok("Заказ подтвержден", res));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable Long id,
            @RequestBody(required = false) CancelOrderRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        String reason = (req != null && req.getReason() != null) ? req.getReason() : "Отмена по инициативе клиента/менеджера";
        OrderResponse res = orderService.cancelOrder(
                id,
                principal.getUsername(),
                principal.getRole(),
                reason
        );
        return ResponseEntity.ok(ApiResponse.ok("Заказ отменен", res));
    }

    @PostMapping("/{id}/complete-pickup")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> completePickup(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        OrderResponse res = orderService.changeStatus(
                id,
                OrderStatus.COMPLETED,
                principal.getUsername(),
                principal.getRole(),
                "Заказ самовывоза выдан клиенту"
        );
        return ResponseEntity.ok(ApiResponse.ok("Заказ самовывоза завершен", res));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<StatusHistoryResponse>>> getOrderHistory(@PathVariable Long id) {
        OrderResponse res = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.ok(res.getHistory()));
    }
}
