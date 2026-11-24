package com.pizzaflow.kitchen.controller;

import com.pizzaflow.common.ApiResponse;
import com.pizzaflow.kitchen.dto.CategoryRequest;
import com.pizzaflow.kitchen.dto.CategoryResponse;
import com.pizzaflow.kitchen.service.MenuService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final MenuService menuService;

    public CategoryController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.ok(menuService.getActiveCategories()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Категория создана", menuService.createCategory(req)));
    }
}
