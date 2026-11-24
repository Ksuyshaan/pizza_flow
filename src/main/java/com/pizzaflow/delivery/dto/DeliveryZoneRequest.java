package com.pizzaflow.delivery.dto;

import java.math.BigDecimal;

public class DeliveryZoneRequest {
    private String name;
    private BigDecimal deliveryFee;
    private BigDecimal freeDeliveryThreshold;
    private int estimatedMinutes;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(BigDecimal deliveryFee) { this.deliveryFee = deliveryFee; }
    public BigDecimal getFreeDeliveryThreshold() { return freeDeliveryThreshold; }
    public void setFreeDeliveryThreshold(BigDecimal freeDeliveryThreshold) { this.freeDeliveryThreshold = freeDeliveryThreshold; }
    public int getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(int estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }
}
