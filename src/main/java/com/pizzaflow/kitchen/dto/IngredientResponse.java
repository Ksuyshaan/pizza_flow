package com.pizzaflow.kitchen.dto;

import com.pizzaflow.kitchen.entity.Ingredient;
import com.pizzaflow.kitchen.entity.IngredientUnit;
import java.math.BigDecimal;

public class IngredientResponse {
    private Long id;
    private String name;
    private IngredientUnit unit;
    private BigDecimal stockQuantity;
    private BigDecimal minStockThreshold;
    private BigDecimal costPerUnit;
    private Long version;
    private boolean lowStock;

    public IngredientResponse() {
    }

    public static IngredientResponse fromEntity(Ingredient ingredient) {
        IngredientResponse res = new IngredientResponse();
        res.setId(ingredient.getId());
        res.setName(ingredient.getName());
        res.setUnit(ingredient.getUnit());
        res.setStockQuantity(ingredient.getStockQuantity());
        res.setMinStockThreshold(ingredient.getMinStockThreshold());
        res.setCostPerUnit(ingredient.getCostPerUnit());
        res.setVersion(ingredient.getVersion());
        res.setLowStock(ingredient.getStockQuantity().compareTo(ingredient.getMinStockThreshold()) <= 0);
        return res;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public IngredientUnit getUnit() {
        return unit;
    }

    public void setUnit(IngredientUnit unit) {
        this.unit = unit;
    }

    public BigDecimal getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(BigDecimal stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public BigDecimal getMinStockThreshold() {
        return minStockThreshold;
    }

    public void setMinStockThreshold(BigDecimal minStockThreshold) {
        this.minStockThreshold = minStockThreshold;
    }

    public BigDecimal getCostPerUnit() {
        return costPerUnit;
    }

    public void setCostPerUnit(BigDecimal costPerUnit) {
        this.costPerUnit = costPerUnit;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public boolean isLowStock() {
        return lowStock;
    }

    public void setLowStock(boolean lowStock) {
        this.lowStock = lowStock;
    }
}
