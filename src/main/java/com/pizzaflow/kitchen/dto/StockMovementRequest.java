package com.pizzaflow.kitchen.dto;

import com.pizzaflow.kitchen.entity.MovementType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class StockMovementRequest {

    @NotNull(message = "Ингредиент обязателен")
    private Long ingredientId;

    @NotNull(message = "Тип операции обязателен (RECEIPT, CONSUMPTION, ADJUSTMENT, WASTE)")
    private MovementType movementType;

    @NotNull(message = "Количество обязательно")
    @DecimalMin(value = "0.01", message = "Количество должно быть больше 0")
    private BigDecimal quantity;

    private String reason;
    private String referenceType;
    private String referenceId;

    public StockMovementRequest() {
    }

    public StockMovementRequest(Long ingredientId, MovementType movementType, BigDecimal quantity, String reason) {
        this.ingredientId = ingredientId;
        this.movementType = movementType;
        this.quantity = quantity;
        this.reason = reason;
    }

    public Long getIngredientId() {
        return ingredientId;
    }

    public void setIngredientId(Long ingredientId) {
        this.ingredientId = ingredientId;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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
}
