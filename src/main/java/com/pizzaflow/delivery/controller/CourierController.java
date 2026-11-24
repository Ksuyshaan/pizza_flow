package com.pizzaflow.delivery.controller;

import com.pizzaflow.common.ApiResponse;
import com.pizzaflow.delivery.dto.CourierResponse;
import com.pizzaflow.delivery.service.DeliveryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/couriers")
public class CourierController {

    private final DeliveryService deliveryService;

    public CourierController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'COURIER')")
    public ResponseEntity<ApiResponse<List<CourierResponse>>> getAllCouriers() {
        return ResponseEntity.ok(ApiResponse.ok(deliveryService.getAllCouriers()));
    }

    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<CourierResponse>>> getAvailableCouriersFifo() {
        return ResponseEntity.ok(ApiResponse.ok(deliveryService.getAvailableCouriersFifo()));
    }
}
