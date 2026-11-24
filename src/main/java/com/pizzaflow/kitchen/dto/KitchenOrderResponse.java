package com.pizzaflow.kitchen.dto;

import com.pizzaflow.kitchen.entity.KitchenOrder;
import com.pizzaflow.kitchen.entity.KitchenOrderStatus;
import com.pizzaflow.order.dto.OrderItemResponse;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class KitchenOrderResponse {
    private Long id;
    private Long orderId;
    private String orderNumber;
    private KitchenOrderStatus status;
    private int priority;
    private int estimatedCookingMinutes;
    private Instant startedAt;
    private Instant completedAt;
    private String cookUsername;
    private List<OrderItemResponse> items;

    public KitchenOrderResponse() {
    }

    public static KitchenOrderResponse fromEntity(KitchenOrder ko) {
        KitchenOrderResponse res = new KitchenOrderResponse();
        res.setId(ko.getId());
        if (ko.getOrder() != null) {
            res.setOrderId(ko.getOrder().getId());
            res.setOrderNumber(ko.getOrder().getOrderNumber());
            if (ko.getOrder().getItems() != null) {
                res.setItems(ko.getOrder().getItems().stream()
                        .map(OrderItemResponse::fromEntity)
                        .collect(Collectors.toList()));
            }
        }
        res.setStatus(ko.getStatus());
        res.setPriority(ko.getPriority());
        res.setEstimatedCookingMinutes(ko.getEstimatedCookingMinutes());
        res.setStartedAt(ko.getStartedAt());
        res.setCompletedAt(ko.getCompletedAt());
        res.setCookUsername(ko.getCookUsername());
        return res;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public KitchenOrderStatus getStatus() {
        return status;
    }

    public void setStatus(KitchenOrderStatus status) {
        this.status = status;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getEstimatedCookingMinutes() {
        return estimatedCookingMinutes;
    }

    public void setEstimatedCookingMinutes(int estimatedCookingMinutes) {
        this.estimatedCookingMinutes = estimatedCookingMinutes;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getCookUsername() {
        return cookUsername;
    }

    public void setCookUsername(String cookUsername) {
        this.cookUsername = cookUsername;
    }

    public List<OrderItemResponse> getItems() {
        return items;
    }

    public void setItems(List<OrderItemResponse> items) {
        this.items = items;
    }
}
