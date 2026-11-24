package com.pizzaflow.kitchen.entity;

import com.pizzaflow.order.entity.Order;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "kitchen_orders")
public class KitchenOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KitchenOrderStatus status = KitchenOrderStatus.PENDING;

    @Column(nullable = false)
    private int priority = 1;

    @Column(name = "estimated_cooking_minutes", nullable = false)
    private int estimatedCookingMinutes = 15;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cook_username")
    private String cookUsername;

    public KitchenOrder() {
    }

    public KitchenOrder(Order order, int priority, int estimatedCookingMinutes) {
        this.order = order;
        this.status = KitchenOrderStatus.PENDING;
        this.priority = priority;
        this.estimatedCookingMinutes = estimatedCookingMinutes;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
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
}
