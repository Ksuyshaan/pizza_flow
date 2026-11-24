package com.pizzaflow.kitchen.dto;

import com.pizzaflow.kitchen.entity.MovementType;
import com.pizzaflow.kitchen.entity.StockMovement;

import java.math.BigDecimal;
import java.time.Instant;

public class StockMovementResponse {
    private Long id;
    private Long ingredientId;
    private String ingredientName;
    private String unit;
    private MovementType movementType;
    private BigDecimal quantity;
    private BigDecimal balanceAfter;
    private String referenceType;
    private String referenceId;
    private String reason;
    private String actor;
    private Instant createdAt;

    public StockMovementResponse() {
    }

    public static StockMovementResponse fromEntity(StockMovement movement) {
        StockMovementResponse res = new StockMovementResponse();
        res.setId(movement.getId());
        if (movement.getIngredient() != null) {
            res.setIngredientId(movement.getIngredient().getId());
            res.setIngredientName(movement.getIngredient().getName());
            res.setUnit(movement.getIngredient().getUnit().name());
        }
        res.setMovementType(movement.getMovementType());
        res.setQuantity(movement.getQuantity());
        res.setBalanceAfter(movement.getBalanceAfter());
        res.setReferenceType(movement.getReferenceType());
        res.setReferenceId(movement.getReferenceId());
        res.setReason(movement.getReason());
        res.setActor(movement.getActor());
        res.setCreatedAt(movement.getCreatedAt());
        return res;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIngredientId() {
        return ingredientId;
    }

    public void setIngredientId(Long ingredientId) {
        this.ingredientId = ingredientId;
    }

    public String getIngredientName() {
        return ingredientName;
    }

    public void setIngredientName(String ingredientName) {
        this.ingredientName = ingredientName;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public MovementType getMovementType() {
        return movementType;
    }

    public void setMovementType(MovementType movementType) {
        this.movementType = movementType;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
