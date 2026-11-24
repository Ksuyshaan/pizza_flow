package com.pizzaflow.delivery.dto;

import com.pizzaflow.delivery.entity.DeliveryZone;

import java.math.BigDecimal;

public class DeliveryZoneResponse {
    private Long id;
    private String name;
    private BigDecimal deliveryFee;
    private BigDecimal freeDeliveryThreshold;
    private int estimatedMinutes;
    private boolean active;

    public static DeliveryZoneResponse fromEntity(DeliveryZone z) {
        DeliveryZoneResponse r = new DeliveryZoneResponse();
        r.id = z.getId();
        r.name = z.getName();
        r.deliveryFee = z.getDeliveryFee();
        r.freeDeliveryThreshold = z.getFreeDeliveryThreshold();
        r.estimatedMinutes = z.getEstimatedMinutes();
        r.active = z.isActive();
        return r;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(BigDecimal deliveryFee) { this.deliveryFee = deliveryFee; }
    public BigDecimal getFreeDeliveryThreshold() { return freeDeliveryThreshold; }
    public void setFreeDeliveryThreshold(BigDecimal freeDeliveryThreshold) { this.freeDeliveryThreshold = freeDeliveryThreshold; }
    public int getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(int estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
