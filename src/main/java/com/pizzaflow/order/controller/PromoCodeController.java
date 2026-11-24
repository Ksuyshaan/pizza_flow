package com.pizzaflow.order.controller;

import com.pizzaflow.common.ApiResponse;
import com.pizzaflow.order.dto.CreatePromoCodeRequest;
import com.pizzaflow.order.dto.PromoCodeResponse;
import com.pizzaflow.order.dto.PromoValidationResponse;
import com.pizzaflow.order.dto.ValidatePromoRequest;
import com.pizzaflow.order.service.PromoCodeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/promocodes")
public class PromoCodeController {

    private final PromoCodeService promoCodeService;

    public PromoCodeController(PromoCodeService promoCodeService) {
        this.promoCodeService = promoCodeService;
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<PromoValidationResponse>> validatePromo(@Valid @RequestBody ValidatePromoRequest req) {
        PromoValidationResponse res = promoCodeService.validate(req.getCode(), req.getOrderAmount());
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<PromoCodeResponse>>> getAllPromoCodes() {
        List<PromoCodeResponse> list = promoCodeService.getAll();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<PromoCodeResponse>> createPromoCode(@Valid @RequestBody CreatePromoCodeRequest req) {
        PromoCodeResponse res = promoCodeService.create(req);
        return ResponseEntity.ok(ApiResponse.ok("Промокод создан", res));
    }
}
