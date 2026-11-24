package com.pizzaflow.kitchen.dto;

import com.pizzaflow.kitchen.entity.RecipeItem;
import java.math.BigDecimal;

public class RecipeItemDto {
    private Long id;
    private Long ingredientId;
    private String ingredientName;
    private String unit;
    private BigDecimal quantityRequired;

    public RecipeItemDto() {
    }

    public RecipeItemDto(Long ingredientId, BigDecimal quantityRequired) {
        this.ingredientId = ingredientId;
        this.quantityRequired = quantityRequired;
    }

    public static RecipeItemDto fromEntity(RecipeItem item) {
        RecipeItemDto dto = new RecipeItemDto();
        dto.setId(item.getId());
        if (item.getIngredient() != null) {
            dto.setIngredientId(item.getIngredient().getId());
            dto.setIngredientName(item.getIngredient().getName());
            dto.setUnit(item.getIngredient().getUnit().name());
        }
        dto.setQuantityRequired(item.getQuantityRequired());
        return dto;
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

    public BigDecimal getQuantityRequired() {
        return quantityRequired;
    }

    public void setQuantityRequired(BigDecimal quantityRequired) {
        this.quantityRequired = quantityRequired;
    }
}
