package com.pizzaflow.kitchen.controller;

import com.pizzaflow.common.ApiResponse;
import com.pizzaflow.kitchen.dto.KitchenLoadResponse;
import com.pizzaflow.kitchen.dto.KitchenOrderResponse;
import com.pizzaflow.kitchen.entity.KitchenOrderStatus;
import com.pizzaflow.kitchen.service.KitchenService;
import com.pizzaflow.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/kitchen")
public class KitchenController {

    private final KitchenService kitchenService;

    public KitchenController(KitchenService kitchenService) {
        this.kitchenService = kitchenService;
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAnyRole('COOK', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<KitchenOrderResponse>>> getKitchenOrders(
            @RequestParam(required = false) KitchenOrderStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(kitchenService.getKitchenOrders(status)));
    }

    @PostMapping("/orders/{id}/start")
    @PreAuthorize("hasAnyRole('COOK', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<KitchenOrderResponse>> startCooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        KitchenOrderResponse res = kitchenService.startCooking(id, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Приготовление начато, сырье списано со склада", res));
    }

    @PostMapping("/orders/{id}/complete")
    @PreAuthorize("hasAnyRole('COOK', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<KitchenOrderResponse>> completeCooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        KitchenOrderResponse res = kitchenService.completeCooking(id, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Приготовление завершено, заказ готов", res));
    }

    @GetMapping("/load")
    public ResponseEntity<ApiResponse<KitchenLoadResponse>> getKitchenLoad() {
        return ResponseEntity.ok(ApiResponse.ok(kitchenService.getKitchenLoad()));
    }
}
