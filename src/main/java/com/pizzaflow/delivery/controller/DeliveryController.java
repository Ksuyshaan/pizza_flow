package com.pizzaflow.delivery.controller;

import com.pizzaflow.delivery.dto.CourierResponse;
import com.pizzaflow.delivery.dto.DeliveryResponse;
import com.pizzaflow.delivery.dto.DeliveryZoneRequest;
import com.pizzaflow.delivery.dto.DeliveryZoneResponse;
import com.pizzaflow.delivery.entity.DeliveryStatus;
import com.pizzaflow.delivery.service.DeliveryService;
import com.pizzaflow.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/delivery")
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    // ── Zones ──────────────────────────────────────────────────────────────────
    @GetMapping("/zones")
    public ResponseEntity<List<DeliveryZoneResponse>> getZones() {
        return ResponseEntity.ok(deliveryService.getActiveZones());
    }

    @PostMapping("/zones")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<DeliveryZoneResponse> createZone(@RequestBody DeliveryZoneRequest req) {
        return ResponseEntity.ok(deliveryService.createZone(req));
    }

    // ── Couriers ──────────────────────────────────────────────────────────────
    @GetMapping("/couriers")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','COURIER')")
    public ResponseEntity<List<CourierResponse>> getCouriers() {
        return ResponseEntity.ok(deliveryService.getAllCouriers());
    }

    @GetMapping("/couriers/available")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','COURIER')")
    public ResponseEntity<List<CourierResponse>> getAvailableCouriers() {
        return ResponseEntity.ok(deliveryService.getAvailableCouriersFifo());
    }

    // ── Deliveries ──────────────────────────────────────────────────────────────
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','COURIER')")
    public ResponseEntity<List<DeliveryResponse>> getDeliveries(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long courierUserId) {
        DeliveryStatus ds = status != null ? DeliveryStatus.valueOf(status) : null;
        return ResponseEntity.ok(deliveryService.getDeliveries(ds, courierUserId));
    }

    @PostMapping("/{deliveryId}/assign")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<DeliveryResponse> manualAssign(
            @PathVariable Long deliveryId,
            @RequestParam Long courierId) {
        return ResponseEntity.ok(deliveryService.manualAssignCourier(deliveryId, courierId));
    }

    @PostMapping("/{deliveryId}/auto-assign")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<DeliveryResponse> autoAssign(@PathVariable Long deliveryId) {
        return ResponseEntity.ok(deliveryService.autoAssignCourier(deliveryId));
    }

    @PostMapping("/{deliveryId}/pickup")
    @PreAuthorize("hasRole('COURIER')")
    public ResponseEntity<DeliveryResponse> pickup(
            @PathVariable Long deliveryId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(deliveryService.pickupDelivery(deliveryId, principal.getUsername()));
    }

    @PostMapping("/{deliveryId}/complete")
    @PreAuthorize("hasRole('COURIER')")
    public ResponseEntity<DeliveryResponse> complete(
            @PathVariable Long deliveryId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(deliveryService.completeDelivery(deliveryId, principal.getUsername()));
    }
}
