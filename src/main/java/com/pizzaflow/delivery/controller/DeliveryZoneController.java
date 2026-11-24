package com.pizzaflow.delivery.controller;

import com.pizzaflow.common.ApiResponse;
import com.pizzaflow.delivery.dto.DeliveryZoneRequest;
import com.pizzaflow.delivery.dto.DeliveryZoneResponse;
import com.pizzaflow.delivery.service.DeliveryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/delivery-zones")
public class DeliveryZoneController {

    private final DeliveryService deliveryService;

    public DeliveryZoneController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DeliveryZoneResponse>>> getZones() {
        return ResponseEntity.ok(ApiResponse.ok(deliveryService.getActiveZones()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DeliveryZoneResponse>> createZone(@Valid @RequestBody DeliveryZoneRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Зона доставки создана", deliveryService.createZone(req)));
    }
}
