package com.pizzaflow.kitchen.controller;

import com.pizzaflow.common.ApiResponse;
import com.pizzaflow.kitchen.dto.IngredientResponse;
import com.pizzaflow.kitchen.dto.StockMovementRequest;
import com.pizzaflow.kitchen.dto.StockMovementResponse;
import com.pizzaflow.kitchen.entity.MovementType;
import com.pizzaflow.kitchen.service.InventoryService;
import com.pizzaflow.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/ingredients")
    @PreAuthorize("hasAnyRole('COOK', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<IngredientResponse>>> getIngredients() {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getAllIngredients()));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<IngredientResponse>>> getLowStock() {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getLowStockIngredients()));
    }

    @PostMapping("/movements")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<StockMovementResponse>> createMovement(
            @Valid @RequestBody StockMovementRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        StockMovementResponse res = inventoryService.createMovement(req, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Складская операция зафиксирована", res));
    }

    @GetMapping("/movements")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<StockMovementResponse>>> getMovements(
            @RequestParam(required = false) Long ingredientId,
            @RequestParam(required = false) MovementType type) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getMovements(ingredientId, type)));
    }
}
